package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
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
