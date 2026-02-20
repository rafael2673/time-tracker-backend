package com.ap101gamestudio.timetracker.model;

public class WorkPolicyTest {

    public void shouldThrowExceptionWhenNameIsBlank() {
        boolean exceptionThrown = false;
        try {
            new WorkPolicy("", 480, 10);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new AssertionError("Expected IllegalArgumentException for blank name");
        }
    }

    public void shouldCreateWorkPolicySuccessfully() {
        WorkPolicy policy = new WorkPolicy("Standard CLT", 480, 10);
        if (!"Standard CLT".equals(policy.getName())) {
            throw new AssertionError("Policy name mismatch");
        }
    }

    public void runAll() {
        shouldThrowExceptionWhenNameIsBlank();
        shouldCreateWorkPolicySuccessfully();
    }
}
