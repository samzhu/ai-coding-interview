package com.interview.question;

public record ProjectFileDetail(
        String id,
        String filePath,
        String content,
        boolean editable,
        int sortOrder) {
}
