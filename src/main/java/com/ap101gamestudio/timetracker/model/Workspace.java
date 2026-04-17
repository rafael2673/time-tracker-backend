package com.ap101gamestudio.timetracker.model;

import com.ap101gamestudio.timetracker.model.enums.ClosureCountType;
import com.ap101gamestudio.timetracker.model.enums.ClosurePendingStrategy;
import com.ap101gamestudio.timetracker.model.enums.ClosureShiftRule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "radius_meters", nullable = false)
    private Integer radiusMeters;

    @Column(name = "auto_closure_enabled", nullable = false)
    private boolean autoClosureEnabled = false;

    @Column(name = "closure_target_day")
    private Integer closureTargetDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "closure_count_type")
    private ClosureCountType closureCountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "closure_shift_rule")
    private ClosureShiftRule closureShiftRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "closure_pending_strategy")
    private ClosurePendingStrategy closurePendingStrategy;

    @Column(name = "state_uf", length = 2)
    private String stateUf;

    @Column(name = "ibge_code", length = 7)
    private String ibgeCode;

    protected Workspace() {
    }

    public Workspace(String name, Double latitude, Double longitude, Integer radiusMeters) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workspace name cannot be null or blank");
        }
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Invalid latitude");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Invalid longitude");
        }
        if (radiusMeters == null || radiusMeters <= 0) {
            throw new IllegalArgumentException("Radius must be greater than zero");
        }
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
    }
}
