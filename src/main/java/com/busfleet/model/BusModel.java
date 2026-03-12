package com.busfleet.model;

/**
 * Модели автобусов парка.
 * В будущем можно добавлять новые модели.
 */
public enum BusModel {
    /** Оставлен для старых записей в БД — при добавлении нового транспорта выбирать Мерс или Китай. */
    MAZ_203("МАЗ-203"),
    MAZ_203_MERS("МАЗ-203 (Мерс)"),
    MAZ_203_CHINA("МАЗ-203 (Китай)"),
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
