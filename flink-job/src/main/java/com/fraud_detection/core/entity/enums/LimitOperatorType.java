package com.fraud_detection.core.entity.enums;

public enum LimitOperatorType {
    EQUAL("="), NOT_EQUAL("!="), GREATER_EQUAL(">="), LESS_EQUAL("<="), GREATER(">"), LESS("<");

    String operator;

    LimitOperatorType(String operator) {
        this.operator = operator;
    }

    public static LimitOperatorType fromString(String text) {
        for (LimitOperatorType b : LimitOperatorType.values()) {
            if (b.operator.equals(text)) {
                return b;
            }
        }
        return null;
    }
}
