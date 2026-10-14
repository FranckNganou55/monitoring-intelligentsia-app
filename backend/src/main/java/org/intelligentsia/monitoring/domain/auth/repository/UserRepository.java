package org.intelligentsia.monitoring.domain.auth.repository;

import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    List<User> findAllByStatus(UserStatus status);

    @Query("""
            SELECT u FROM User u
            WHERE (
                COALESCE(:search, '') = ''
                OR LOWER(u.login) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%'))
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%'))
            )
            AND (:status IS NULL OR u.status = :status)
            AND (:institutionId IS NULL OR u.institution.id = :institutionId)
            """)
    Page<User> findWithFilters(
        String search, 
        UserStatus status, 
        Long institutionId, 
        Pageable pageable
    );
}
