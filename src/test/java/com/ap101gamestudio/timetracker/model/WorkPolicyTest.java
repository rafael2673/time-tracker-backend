package com.ap101gamestudio.timetracker.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WorkPolicyTest {

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new WorkPolicy("", 480, 10));
    }

    @Test
    void shouldCreateWorkPolicySuccessfully() {
        WorkPolicy policy = new WorkPolicy("Standard CLT", 480, 10);
        Assertions.assertEquals("Standard CLT", policy.getName());
    }
}