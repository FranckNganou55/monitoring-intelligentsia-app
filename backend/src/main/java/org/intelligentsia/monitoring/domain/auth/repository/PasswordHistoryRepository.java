package org.intelligentsia.monitoring.domain.auth.repository;

import org.intelligentsia.monitoring.domain.auth.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    List<PasswordHistory> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
