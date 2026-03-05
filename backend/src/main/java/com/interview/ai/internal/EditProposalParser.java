package com.interview.ai.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 AI 回應文字中的 edit_proposal 區塊。
 *
 * 設計說明：
 * AI 在 system prompt 中被要求以下列格式輸出修改建議：
 *
 * <pre>
 * &lt;edit_proposal file="path/to/file"&gt;
 * &lt;&lt;&lt;&lt;&lt;&lt;&lt; ORIGINAL
 * 原始程式碼
 * =======
 * 建議修改後的程式碼
 * &gt;&gt;&gt;&gt;&gt;&gt;&gt; PROPOSED
 * &lt;/edit_proposal&gt;
 * </pre>
 *
 * Parser 採容錯設計：解析失敗的區塊會被忽略（記錄 warn log），
 * 不影響 AI 回應的正常文字顯示。
 */
@Component
public class EditProposalParser {

    private static final Logger log = LoggerFactory.getLogger(EditProposalParser.class);

    // 匹配完整的 edit_proposal 區塊，DOTALL 讓 . 也能匹配換行
    private static final Pattern PROPOSAL_PATTERN = Pattern.compile(
            "<edit_proposal\\s+file=\"([^\"]+)\">(.*?)</edit_proposal>",
            Pattern.DOTALL
    );

    // 分隔 ORIGINAL 和 PROPOSED 區段
    private static final Pattern CONTENT_PATTERN = Pattern.compile(
            "<<<<<<< ORIGINAL\\s*\\n(.*?)\\n=======\\s*\\n(.*?)\\n>>>>>>> PROPOSED",
            Pattern.DOTALL
    );

    /**
     * 解析 AI 完整回應文字，提取所有編輯提案。
     *
     * @param aiResponse AI 的完整回應字串（串流結束後的累計文字）
     * @return 解析出的編輯提案清單，若無提案或解析失敗則回傳空清單
     */
    public List<EditProposal> parse(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return List.of();
        }

        List<EditProposal> proposals = new ArrayList<>();
        Matcher proposalMatcher = PROPOSAL_PATTERN.matcher(aiResponse);

        while (proposalMatcher.find()) {
            String filePath = proposalMatcher.group(1).trim();
            String blockContent = proposalMatcher.group(2);

            Matcher contentMatcher = CONTENT_PATTERN.matcher(blockContent);
            if (contentMatcher.find()) {
                String original = contentMatcher.group(1);
                String proposed = contentMatcher.group(2);
                proposals.add(new EditProposal(filePath, original, proposed));
            } else {
                log.warn("EditProposalParser: found edit_proposal block for '{}' but could not parse ORIGINAL/PROPOSED sections", filePath);
            }
        }

        return proposals;
    }
}
