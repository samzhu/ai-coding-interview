package com.interview.execution.internal;

import com.github.dockerjava.api.DockerClient;
import com.interview.execution.TerminalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 使用 Docker daemon 原始 Socket（TCP 或 Unix）實作的互動式終端 session。
 *
 * 設計說明（為什麼不用 docker-java execStartCmd 也不用 docker CLI ProcessBuilder）：
 *
 * 問題一：docker-java httpclient5 transport 不支援 HTTP hijacking。
 * Docker exec 的 stdin 需要 HTTP hijacking（daemon 將 HTTP 連線升級為原始 TCP 雙向串流）。
 * docker-java 的 httpclient5 transport 不支援此協定，導致 stdin 只能送出第一批資料後就中斷。
 * 這是 docker-java 已知問題（#1552）。
 *
 * 問題二：ProcessBuilder 呼叫 docker CLI 在容器化部署中無效。
 * 後端本身在 Docker 容器內執行，容器內沒有安裝 docker CLI，ProcessBuilder 無法找到可執行檔。
 *
 * 解決方案：自行實作 HTTP hijacking。
 * 透過 Java 21 原生 SocketChannel（支援 Unix domain socket 和 TCP socket）
 * 直連 Docker daemon，手動發送 HTTP exec/start 請求，並在收到 101 UPGRADED 後
 * 將整個 TCP 連線作為雙向 stdin/stdout 管道使用。
 *
 * 為何使用 Tty=true（PTY 模式）：
 * TTY 模式下 Docker 送出原始位元組（無 8-byte 多工 frame），
 * 使用者可享有完整的 readline 行編輯、Tab 補全、ANSI 顏色等體驗。
 * xterm.js 在瀏覽器端負責終端模擬，server 端只需管道可靠傳遞位元組。
 * resize 透過 docker-java 的 resizeExecCmd API 實作（短暫 HTTP POST，不需 hijacking）。
 */
class DockerTerminalSession implements TerminalSession {

    private static final Logger log = LoggerFactory.getLogger(DockerTerminalSession.class);
    private static final int MAX_BUFFER_BYTES = 64 * 1024;

    private final String id;
    private final String execId;
    private final SocketChannel channel;
    private final InputStream channelIn;
    private final OutputStream channelOut;
    private final DockerClient dockerClient;

    // synchronized(this) 保護 outputCallback 和 outputBuffer
    private Consumer<byte[]> outputCallback;
    private final List<byte[]> outputBuffer = new ArrayList<>();
    private int bufferedBytes = 0;

    private volatile boolean alive = true;

    DockerTerminalSession(String id, String execId, String dockerHost, DockerClient dockerClient) {
        this.id = id;
        this.execId = execId;
        this.dockerClient = dockerClient;

        try {
            // 開啟原始 socket 連線至 Docker daemon（TCP 或 Unix domain socket）
            this.channel = openDockerSocket(dockerHost);
            // SocketChannel 預設為 blocking mode，Channels.newInputStream/newOutputStream
            // 包裝為標準 InputStream/OutputStream，適合在虛擬執行緒中使用
            this.channelIn = Channels.newInputStream(channel);
            this.channelOut = Channels.newOutputStream(channel);

            // 發送 HTTP exec/start 請求，帶 Connection: Upgrade / Upgrade: tcp header
            // 觸發 Docker daemon 的 HTTP hijacking 協定升級
            sendExecStartRequest(execId);

            // 讀取並丟棄 HTTP 回應 headers（期望收到 "HTTP/1.1 101 UPGRADED"）
            // 之後的連線即為原始 TCP 雙向串流
            readHttpResponseHeaders();

        } catch (IOException e) {
            throw new RuntimeException("Failed to create terminal session: " + e.getMessage(), e);
        }

        // 虛擬執行緒持續讀取 Docker exec stdout（TTY 模式：原始位元組，無多工 frame header）
        // 使用虛擬執行緒避免 OS thread 在 I/O 阻塞時佔用資源
        Thread.ofVirtual().name("terminal-reader-" + id).start(() -> {
            try {
                byte[] buf = new byte[4096];
                int n;
                while ((n = channelIn.read(buf)) != -1) {
                    deliverOutput(Arrays.copyOf(buf, n));
                }
            } catch (IOException e) {
                log.debug("Terminal session {} reader ended: {}", id, e.getMessage());
            } finally {
                alive = false;
            }
        });

        log.info("Terminal session {} started (execId={})", id, execId);
    }

    /**
     * 根據 DOCKER_HOST 格式開啟對應的 socket channel。
     *
     * Java 21 原生支援 Unix domain socket（java.net.UnixDomainSocketAddress）；
     * TCP socket 使用標準 InetSocketAddress。
     * 兩者皆透過 SocketChannel API 統一處理，行為完全一致。
     */
    private static SocketChannel openDockerSocket(String dockerHost) throws IOException {
        if (dockerHost.startsWith("unix://")) {
            // unix:///var/run/docker.sock → /var/run/docker.sock
            String path = dockerHost.substring("unix://".length());
            return SocketChannel.open(UnixDomainSocketAddress.of(path));
        }
        // tcp:// 或 http:// → TCP socket
        URI uri = URI.create(dockerHost);
        int port = uri.getPort() > 0 ? uri.getPort() : 2375;
        return SocketChannel.open(new InetSocketAddress(uri.getHost(), port));
    }

    /**
     * 發送 HTTP POST /exec/{execId}/start 並帶入 HTTP hijacking headers。
     *
     * Connection: Upgrade + Upgrade: tcp 是觸發 Docker daemon 協定升級的關鍵。
     * Docker daemon 在收到此 header 後，將 HTTP 連線升級為原始 TCP 雙向串流。
     * Tty=true 告知 daemon 以 PTY 模式執行（原始位元組輸出，無多工 frame header）。
     */
    private void sendExecStartRequest(String execId) throws IOException {
        String body = "{\"Detach\":false,\"Tty\":true}";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String headers =
                "POST /exec/" + execId + "/start HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: Upgrade\r\n" +
                "Upgrade: tcp\r\n" +
                "\r\n";
        channelOut.write(headers.getBytes(StandardCharsets.UTF_8));
        channelOut.write(bodyBytes);
        channelOut.flush();
    }

    /**
     * 讀取 HTTP 回應 headers，確認升級成功（101 UPGRADED）。
     *
     * 逐位元組讀取直到遇到 \r\n\r\n（HTTP header 結束標誌），
     * 之後的所有資料都是原始 PTY 輸出，由 reader 執行緒接手處理。
     */
    private void readHttpResponseHeaders() throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] oneByte = new byte[1];
        while (true) {
            int n = channelIn.read(oneByte);
            if (n == -1) throw new IOException("Docker daemon closed connection during HTTP handshake");
            sb.append((char) oneByte[0]);
            if (sb.length() >= 4 && "\r\n\r\n".equals(sb.substring(sb.length() - 4))) break;
            if (sb.length() > 8192) throw new IOException("HTTP response headers too long");
        }
        String statusLine = sb.substring(0, sb.indexOf("\r\n"));
        if (!statusLine.startsWith("HTTP/1.1 101")) {
            throw new IOException("Expected HTTP 101 UPGRADED, got: " + statusLine);
        }
        log.debug("Terminal session {} HTTP hijacking established ({})", id, statusLine);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void write(byte[] data) {
        if (!alive) return;
        try {
            channelOut.write(data);
            channelOut.flush();
        } catch (IOException e) {
            log.debug("Terminal session {} write failed: {}", id, e.getMessage());
            alive = false;
        }
    }

    @Override
    public void onOutput(Consumer<byte[]> callback) {
        List<byte[]> buffered;
        synchronized (this) {
            this.outputCallback = callback;
            buffered = new ArrayList<>(outputBuffer);
            outputBuffer.clear();
            bufferedBytes = 0;
        }
        // 在 synchronized 外呼叫 callback，避免持鎖時阻塞 Docker 輸出讀取執行緒
        for (byte[] chunk : buffered) {
            callback.accept(chunk);
        }
    }

    @Override
    public void resize(int cols, int rows) {
        // TTY 模式下可透過 Docker resizeExec API 調整終端大小（短暫獨立 HTTP 請求，不需 hijacking）
        try {
            dockerClient.resizeExecCmd(execId).withSize(rows, cols).exec();
        } catch (Exception e) {
            log.debug("Terminal session {} resize failed: {}", id, e.getMessage());
        }
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void close() {
        alive = false;
        try {
            // 送出 exit 指令讓 bash 正常退出，再關閉 socket
            channelOut.write("exit\n".getBytes(StandardCharsets.UTF_8));
            channelOut.flush();
        } catch (IOException ignored) {
        }
        try {
            channel.close();
        } catch (IOException ignored) {
        }
        log.info("Terminal session {} closed", id);
    }

    /**
     * 將輸出送達：若 callback 已設定則直接呼叫；否則暫時緩衝。
     * 在 synchronized 外呼叫 callback，避免持鎖時阻塞 Docker 輸出讀取執行緒。
     */
    private void deliverOutput(byte[] payload) {
        Consumer<byte[]> cb;
        synchronized (this) {
            cb = outputCallback;
            if (cb == null) {
                if (bufferedBytes < MAX_BUFFER_BYTES) {
                    outputBuffer.add(payload);
                    bufferedBytes += payload.length;
                }
                return;
            }
        }
        cb.accept(payload);
    }
}
