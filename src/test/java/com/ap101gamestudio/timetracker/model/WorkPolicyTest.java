package com.ap101gamestudio.timetracker.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WorkPolicyTest {

    private Workspace createWorkspace() {
        return new Workspace(
                "Office",
                -5.7945,
                -35.2110,
                100
        );
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        Workspace workspace = createWorkspace();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new WorkPolicy(workspace, "", 480, 10, "MONDAY,TUESDAY")
        );
    }

    @Test
    void shouldCreateWorkPolicySuccessfully() {
        Workspace workspace = createWorkspace();

        WorkPolicy policy = new WorkPolicy(
                workspace,
                "Standard CLT",
                480,
                10,
                "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
        );

        Assertions.assertEquals("Standard CLT", policy.getName());
    }

    @Test
    void shouldParseWorkingDaysCorrectly() {
        Workspace workspace = createWorkspace();

        WorkPolicy policy = new WorkPolicy(
                workspace,
                "Standard CLT",
                480,
                10,
                "MONDAY,TUESDAY,FRIDAY"
        );

        Assertions.assertEquals(3, policy.getWorkingDaysList().size());
    }
}