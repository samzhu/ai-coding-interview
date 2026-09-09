package com.interview.interview.bdd;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 設計說明：
 * 在 BDD 測試環境下，把 Spring `@Async` 的 `taskExecutor` bean 改為同步執行器，
 * 讓 `ContainerInitializationService.initializeContainerAsync()` 與
 * `CheckpointTestExecutor.executeAndGrade()` 在呼叫端同一條 thread 執行完成。
 *
 * 為什麼這樣做：
 *  - 既有 BDD step：`startInterview()` → 立刻 `getCurrentCheckpoint()`
 *    在 production 走 background thread 初始化 container + checkpoint results；
 *    BDD 在 step 之間沒有等待機制，會看到「No active checkpoint」失敗。
 *  - 改用 SyncTaskExecutor 後，`@Async` 變成 in-thread 呼叫，
 *    當 `startInterview()` 回傳時所有 checkpoint 都已 seed，後續查詢必中。
 *  - 只在 BDD test context 生效（透過 @Import），不影響其他測試或 production。
 *
 * 注意：bean 名稱必須是 `taskExecutor` 才能蓋過 Spring `@EnableAsync` 預設 lookup。
 * 來源：https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async
 */
@TestConfiguration
public class BddAsyncSyncConfig {

    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
