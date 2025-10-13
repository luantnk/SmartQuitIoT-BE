package com.smartquit.smartquitiot.enums;

public enum Operator {
    LT, LE, EQ, GE, GT;

    public static Operator fromSymbol(String s) {
        return switch (s.trim()) {
            case "<"  -> LT;
            case "<=" -> LE;
            case "==" -> EQ;
            case ">=" -> GE;
            case ">"  -> GT;
            default   -> Operator.valueOf(s.toUpperCase());
        };
    }
}
