package com.interview.question.interfaces.rest;

import com.interview.question.ProjectFileDetail;

record ProjectFileResponse(
        String id,
        String filePath,
        String content,
        boolean editable,
        int sortOrder) {

    static ProjectFileResponse from(ProjectFileDetail detail) {
        return new ProjectFileResponse(
                detail.id(),
                detail.filePath(),
                detail.content(),
                detail.editable(),
                detail.sortOrder());
    }
}
