package com.ap101gamestudio.timetracker.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

    @Test
    void shouldCreateWorkspaceSuccessfully() {
        Workspace workspace = new Workspace("Sede", -5.8428, -35.1969, 100);
        Assertions.assertEquals("Sede", workspace.getName());
    }

    @Test
    void shouldThrowExceptionForInvalidLatitude() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Workspace("Sede", 100.0, -35.1969, 100));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Workspace("Sede", -100.0, -35.1969, 100));
    }

    @Test
    void shouldThrowExceptionForInvalidLongitude() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Workspace("Sede", -5.8428, 200.0, 100));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Workspace("Sede", -5.8428, -200.0, 100));
    }

    @Test
    void shouldThrowExceptionForInvalidRadius() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Workspace("Sede", -5.8428, -35.1969, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Workspace("Sede", -5.8428, -35.1969, -50));
    }
}