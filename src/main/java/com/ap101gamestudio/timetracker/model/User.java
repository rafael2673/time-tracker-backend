package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
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
    @Getter
    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(name = "password_hash")
    private String passwordHash;

    @Getter
    @Setter
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Getter
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMembership> memberships;

    @Getter
    @Setter
    @Column(name = "recovery_email", unique = true)
    private String recoveryEmail;

    @Getter
    @Setter
    @Column(name = "has_web_password", nullable = false)
    private boolean hasWebPassword = false;

    @Getter
    @Setter
    @Column(name = "system_admin", nullable = false)
    private boolean systemAdmin = false;

    protected User() {}

    public User(String email, String passwordHash, String fullName) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() { return passwordHash != null ? passwordHash : ""; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}