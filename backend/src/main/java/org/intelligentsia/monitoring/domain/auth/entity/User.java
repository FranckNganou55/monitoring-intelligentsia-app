package org.intelligentsia.monitoring.domain.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String login;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_birth")
    private LocalDate dateBirth;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode = "fr";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserScope scope = UserScope.INSTITUTION;

    @Column(name = "password_expires_at")
    private Instant passwordExpiresAt;

    @Column(name = "account_expires_at")
    private Instant accountExpiresAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by")
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        roles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getCode()));
            role.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getCode())));
        });
        return authorities;
    }

    @Override public String getPassword()                { return passwordHash; }
    @Override public String getUsername()                { return login; }
    @Override public boolean isAccountNonExpired()       { return accountExpiresAt == null || Instant.now().isBefore(accountExpiresAt); }
    @Override public boolean isAccountNonLocked()        { return status != UserStatus.LOCKED; }
    @Override public boolean isCredentialsNonExpired()   { return passwordExpiresAt == null || Instant.now().isBefore(passwordExpiresAt); }
    @Override public boolean isEnabled()                 { return status == UserStatus.ACTIVE; }

    /** Built-in roles define the effective scope, even if an older row is stale. */
    public UserScope getScope() {
        if (roles != null) {
            if (roles.stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getCode()))) {
                return UserScope.ADMIN;
            }
            if (roles.stream().anyMatch(role -> "ROLE_MANAGER".equals(role.getCode()))) {
                return UserScope.GLOBAL;
            }
            if (roles.stream().anyMatch(role -> "ROLE_OPERATOR".equals(role.getCode()))) {
                return UserScope.INSTITUTION;
            }
        }
        return scope;
    }

    public boolean hasInstitution() { return institution != null; }

    public boolean isInstitutionScoped() { return getScope() == UserScope.INSTITUTION; }
}
