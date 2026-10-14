package org.intelligentsia.monitoring.domain.configuration.settings.repository;

import org.intelligentsia.monitoring.domain.configuration.settings.entity.AppSetting;
import org.intelligentsia.monitoring.domain.configuration.settings.entity.SettingGroupe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findByCle(String cle);
    List<AppSetting> findByGroupeOrderByCleAsc(SettingGroupe groupe);
}
