package com.example.student.service;

/**
 * Константы для bulk-операций (сентинелы и сообщения), чтобы не дублировать литералы в коде.
 */
public final class BulkOperationConstants {

    private BulkOperationConstants() {
    }

    /** Маркер строки, на которой демонстративно падает bulk-импорт. */
    public static final String ERROR_SENTINEL = "ERROR";

    public static final String MSG_BULK_GENERIC = "Ошибка в bulk операции";
    public static final String MSG_BULK_GROUP = "Ошибка группы";
    public static final String MSG_BULK_TEACHER = "Ошибка преподавателя";
    public static final String MSG_INVALID_GRADE = "Некорректная оценка";
}
