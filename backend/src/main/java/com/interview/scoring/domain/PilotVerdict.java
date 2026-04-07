package com.interview.scoring.domain;

/**
 * 設計說明：四個 verdict 對應 Pilot Score 設計文件 §3.4 schema 的 enum 值。
 * 順序：DRIVER (最佳) → MOSTLY_DRIVER → MIXED → PASSENGER (最差)。
 * 序列化為 JSON 時用 name() 直接輸出大寫字串。
 */
public enum PilotVerdict {
    DRIVER,
    MOSTLY_DRIVER,
    MIXED,
    PASSENGER
}
