package com.ap101gamestudio.timetracker.model;

import com.ap101gamestudio.timetracker.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(name = "password_hash")
    private String passwordHash;

    @Getter
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_policy_id", nullable = false)
    private WorkPolicy workPolicy;

    protected User() {
    }

    public User(String email, String name, String password, UserRole role) {
        this.email = email;
        this.fullName = name;
        this.role = role;
        this.passwordHash = password;
    }

    public User(String email, String passwordHash, String fullName, UserRole role, User manager, WorkPolicy workPolicy) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.manager = manager;
        this.workPolicy = workPolicy;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    @NonNull
    public String getPassword() {
        return passwordHash != null ? passwordHash : "";
    }

    @Override
    @NonNull
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}