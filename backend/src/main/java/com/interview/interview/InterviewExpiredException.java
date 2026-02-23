package com.interview.interview;

/**
 * 跨模組公開異常：面試時間已超時。
 * 由 interview 模組拋出，可被 ai 等模組的 ExceptionHandler 處理。
 */
public class InterviewExpiredException extends RuntimeException {

    public InterviewExpiredException(String message) {
        super(message);
    }
}
