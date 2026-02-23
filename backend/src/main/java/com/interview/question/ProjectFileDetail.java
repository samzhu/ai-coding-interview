package com.interview.question;

import java.util.UUID;

public record ProjectFileDetail(
        UUID id,
        String filePath,
        String content,
        boolean editable,
        int sortOrder) {
}
