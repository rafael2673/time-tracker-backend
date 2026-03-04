package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "link_codes")
public class LinkCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @ManyToOne
    private User user;

    private LocalDateTime expiresAt;

    public LinkCode() {}

    public LinkCode(String code, User user, LocalDateTime expiresAt) {
        this.code = code;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public User getUser() { return user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}