package com.example.student.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkOperationConstantsTest {

    @Test
    void sentinelAndMessagesAreStable() {
        assertEquals("ERROR", BulkOperationConstants.ERROR_SENTINEL);
        assertEquals("Ошибка в bulk операции", BulkOperationConstants.MSG_BULK_GENERIC);
        assertEquals("Ошибка группы", BulkOperationConstants.MSG_BULK_GROUP);
        assertEquals("Ошибка преподавателя", BulkOperationConstants.MSG_BULK_TEACHER);
        assertEquals("Некорректная оценка", BulkOperationConstants.MSG_INVALID_GRADE);
    }
}
