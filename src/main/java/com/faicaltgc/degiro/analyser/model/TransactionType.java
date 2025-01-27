package com.faicaltgc.degiro.analyser.model;

public enum TransactionType {
    KAUF("Kauf"),
    VERKAUF("Verkauf"),
    SPLIT("Split");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Optionale Methode, um das Enum anhand des Display-Namens zu finden
    public static TransactionType fromDisplayName(String displayName) {
        for (TransactionType type : TransactionType.values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Ungültiger Transaktionstyp: " + displayName);
    }
}
