package com.busfleet.model;

/**
 * Модели автобусов парка.
 * В будущем можно добавлять новые модели.
 */
public enum BusModel {
    MAZ_203("МАЗ-203"),
    MAZ_257("МАЗ-257"),
    MAZ_216("МАЗ-216"),
    ZHONG_TONG("ZHONG TONG");

    private final String displayName;

    BusModel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
