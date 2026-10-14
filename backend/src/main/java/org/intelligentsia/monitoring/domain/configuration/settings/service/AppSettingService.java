package org.intelligentsia.monitoring.domain.configuration.settings.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.settings.dto.AppSettingDto;
import org.intelligentsia.monitoring.domain.configuration.settings.dto.AppSettingUpdateRequest;
import org.intelligentsia.monitoring.domain.configuration.settings.dto.FraudAlertCadenceRequest;
import org.intelligentsia.monitoring.domain.configuration.settings.entity.AppSetting;
import org.intelligentsia.monitoring.domain.configuration.settings.entity.SettingType;
import org.intelligentsia.monitoring.domain.configuration.settings.entity.SettingGroupe;
import org.intelligentsia.monitoring.domain.configuration.settings.repository.AppSettingRepository;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.shared.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppSettingService {

    private final AppSettingRepository repository;

    @Value("${app.settings.encryption-key}")
    private String encryptionKey;

    @Value("${app.settings.encryption-salt}")
    private String encryptionSalt;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PostConstruct
    public void preloadCache() {
        ensureFraudNotificationCadenceSettings();
        repository.findAll().forEach(s -> getString(s.getCle()));
        log.info("AppSettings : {} paramètre(s) chargé(s) en cache", repository.count());
    }

    private void ensureFraudNotificationCadenceSettings() {
        ensureIntegerSetting("alerts.fraud.interval.TYPIQUE", "600000", "Délai de notification — Typique");
        ensureIntegerSetting("alerts.fraud.interval.MOINS_CRITIQUE", "300000", "Délai de notification — Moins critique");
        ensureIntegerSetting("alerts.fraud.interval.MOYEN", "60000", "Délai de notification — Moyen");
        ensureSoundDurationSetting("TYPIQUE", "15000");
        ensureSoundDurationSetting("MOINS_CRITIQUE", "15000");
        ensureSoundDurationSetting("MOYEN", "30000");
        ensureSoundDurationSetting("CRITIQUE", "30000");
    }

    private void ensureSoundDurationSetting(String risk, String defaultValue) {
        String key = "alerts.sound.interval." + risk;
        AppSetting setting = repository.findByCle(key).orElse(null);
        if (setting == null) {
            ensureIntegerSetting(key, defaultValue, "Durée du signal sonore — " + risk);
            setting = repository.findByCle(key).orElse(null);
        }
        if (setting != null) {
            // Existing values used to represent a repeat interval; convert them without a database migration.
            if (!List.of("5000", "10000", "15000", "30000", "60000", "120000", "300000").contains(setting.getValeur())) {
                setting.setValeur(defaultValue);
            }
            setting.setLabel("Durée du signal sonore — " + risk);
            setting.setDescription("Durée totale pendant laquelle le signal sonore est joué après le délai de notification.");
            setting.setUpdatedAt(Instant.now());
            setting.setUpdatedBy("system");
            repository.save(setting);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getAlertSoundCadence() {
        return Map.of(
                "TYPIQUE", getInteger("alerts.sound.interval.TYPIQUE", 15_000),
                "MOINS_CRITIQUE", getInteger("alerts.sound.interval.MOINS_CRITIQUE", 15_000),
                "MOYEN", getInteger("alerts.sound.interval.MOYEN", 30_000),
                "CRITIQUE", getInteger("alerts.sound.interval.CRITIQUE", 30_000)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getFraudAlertCadence() {
        return Map.of(
                "TYPIQUE", getInteger("alerts.fraud.interval.TYPIQUE", 600_000),
                "MOINS_CRITIQUE", getInteger("alerts.fraud.interval.MOINS_CRITIQUE", 300_000),
                "MOYEN", getInteger("alerts.fraud.interval.MOYEN", 60_000),
                "CRITIQUE", 0
        );
    }

    private void ensureIntegerSetting(String key, String value, String label) {
        if (repository.findByCle(key).isPresent()) return;
        AppSetting setting = new AppSetting();
        setting.setCle(key);
        setting.setValeur(value);
        setting.setType(SettingType.INTEGER);
        setting.setGroupe(SettingGroupe.ALERTES);
        setting.setLabel(label);
        setting.setDescription(key.startsWith("alerts.sound.")
                ? "Durée totale pendant laquelle le signal sonore est joué après le délai de notification."
                : "Délai avant le signalement de l’alerte. L’alerte est enregistrée et visible immédiatement.");
        setting.setModifiableParAdmin(true);
        setting.setSensible(false);
        setting.setUpdatedAt(Instant.now());
        setting.setUpdatedBy("system");
        repository.save(setting);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Cacheable(value = "settings", key = "#cle")
    @Transactional(readOnly = true)
    public String getString(String cle) {
        return repository.findByCle(cle)
                .map(s -> s.isSensible() ? decrypt(s.getValeur()) : s.getValeur())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public int getInteger(String cle, int defaultValue) {
        String v = getString(cle);
        if (v == null || v.isBlank()) return defaultValue;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(String cle, boolean defaultValue) {
        String v = getString(cle);
        if (v == null || v.isBlank()) return defaultValue;
        return "true".equalsIgnoreCase(v.trim());
    }

    @Cacheable(value = "settings-group", key = "#groupe.name()")
    @Transactional(readOnly = true)
    public List<AppSettingDto> getByGroupe(SettingGroupe groupe) {
        return repository.findByGroupeOrderByCleAsc(groupe)
                .stream().map(this::toDto).toList();
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Caching(evict = {
        @CacheEvict(value = "settings",       key = "#cle"),
        @CacheEvict(value = "settings-group", allEntries = true)
    })
    @Transactional
    public AppSettingDto update(String cle, AppSettingUpdateRequest req) {
        validateFraudNotificationCadence(cle, req.getValeur());
        if (cle.startsWith("alerts.atm.interface.")) {
            if (cle.equals("alerts.atm.interface.consecutive.samples")) throw new BusinessException("SETTING_READONLY", "Utilisez les réglages DOWN et SIGNOFF.");
            int value;
            try { value = Integer.parseInt(req.getValeur()); } catch (RuntimeException e) { throw new BusinessException("INVALID_ATM_SETTINGS", "Valeur entière requise."); }
            boolean samples = cle.endsWith(".consecutive.samples");
            if (samples ? value < 1 || value > 20 : value < 60000 || value > 900000 || value % 60000 != 0)
                throw new BusinessException("INVALID_ATM_SETTINGS", "Paramètre GAB hors limites.");
        }
        AppSetting setting = repository.findByCle(cle).orElseGet(() -> createMissingSoundDurationSetting(cle));

        if (!setting.isModifiableParAdmin()) {
            throw new BusinessException("SETTING_READONLY", "Ce paramètre ne peut pas être modifié.");
        }

        String valueToStore = setting.isSensible() ? encrypt(req.getValeur()) : req.getValeur();
        setting.setValeur(valueToStore);
        setting.setUpdatedAt(Instant.now());
        setting.setUpdatedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));

        return toDto(repository.save(setting));
    }

    @Caching(evict = {@CacheEvict(value = "settings", allEntries = true), @CacheEvict(value = "settings-group", allEntries = true)})
    @Transactional
    public void updateAtmInterfaceSettings(String status, org.intelligentsia.monitoring.domain.configuration.settings.dto.AtmInterfaceSettingsRequest request) {
        if (!List.of("DOWN", "SIGNOFF").contains(status) || request.samples() < 1 || request.samples() > 20
                || request.intervalMs() < 60000 || request.intervalMs() > 900000 || request.intervalMs() % 60000 != 0)
            throw new BusinessException("INVALID_ATM_SETTINGS", "Choisissez 1 à 20 relevés et une fréquence de 1 à 15 minutes entières.");
        String prefix = "alerts.atm.interface." + status;
        for (var entry : Map.of(prefix + ".consecutive.samples", String.valueOf(request.samples()), prefix + ".interval.ms", String.valueOf(request.intervalMs())).entrySet()) {
            AppSetting setting = repository.findByCle(entry.getKey()).orElseThrow(() -> new ResourceNotFoundException("AppSetting", entry.getKey()));
            setting.setValeur(entry.getValue());
            setting.setUpdatedAt(Instant.now());
            setting.setUpdatedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
            repository.save(setting);
        }
    }

    private AppSetting createMissingSoundDurationSetting(String key) {
        String prefix = "alerts.sound.interval.";
        if (!key.startsWith(prefix)) {
            throw new ResourceNotFoundException("AppSetting", key);
        }

        String risk = key.substring(prefix.length());
        String defaultValue = switch (risk) {
            case "TYPIQUE", "MOINS_CRITIQUE" -> "15000";
            case "MOYEN", "CRITIQUE" -> "30000";
            default -> throw new ResourceNotFoundException("AppSetting", key);
        };
        ensureIntegerSetting(key, defaultValue, "Durée du signal sonore — " + risk);
        return repository.findByCle(key).orElseThrow(() -> new ResourceNotFoundException("AppSetting", key));
    }

    @Caching(evict = {
        @CacheEvict(value = "settings", allEntries = true),
        @CacheEvict(value = "settings-group", allEntries = true)
    })
    @Transactional
    public void updateFraudAlertCadence(FraudAlertCadenceRequest request) {
        Map<String, Long> intervals = java.util.Map.of(
                "TYPIQUE", request.getTypiqueMs(),
                "MOINS_CRITIQUE", request.getMoinsCritiqueMs(),
                "MOYEN", request.getMoyenMs()
        );
        List<String> allowed = List.of("5000", "60000", "300000", "600000", "900000", "1800000", "3600000");
        List<AppSetting> updated = new java.util.ArrayList<>();
        String updatedBy = SecurityUtils.getCurrentUserLogin().orElse("system");
        for (Map.Entry<String, Long> entry : intervals.entrySet()) {
            String key = "alerts.fraud.interval." + entry.getKey();
            String value = String.valueOf(entry.getValue());
            if (!allowed.contains(value)) {
                throw new BusinessException("INVALID_ALERT_CADENCE", "Délai de notification fraude non autorisé.");
            }
            AppSetting setting = repository.findByCle(key)
                    .orElseThrow(() -> new ResourceNotFoundException("AppSetting", key));
            setting.setValeur(value);
            setting.setUpdatedAt(Instant.now());
            setting.setUpdatedBy(updatedBy);
            updated.add(setting);
        }
        repository.saveAll(updated);
    }

    private void validateFraudNotificationCadence(String key, String value) {
        boolean soundDuration = key.startsWith("alerts.sound.interval.");
        if (!key.startsWith("alerts.fraud.interval.") && !soundDuration) return;
        boolean supportedRisk = (soundDuration
                ? List.of("TYPIQUE", "MOINS_CRITIQUE", "MOYEN", "CRITIQUE")
                : List.of("TYPIQUE", "MOINS_CRITIQUE", "MOYEN"))
                .stream().anyMatch(risk -> key.equals((soundDuration ? "alerts.sound.interval." : "alerts.fraud.interval.") + risk));
        List<String> allowed = soundDuration
                ? List.of("5000", "10000", "15000", "30000", "60000", "120000", "300000")
                : List.of("5000", "60000", "300000", "600000", "900000", "1800000", "3600000");
        if (!supportedRisk || !allowed.contains(value)) {
            throw new BusinessException("INVALID_ALERT_CADENCE", "Délai de notification sonore ou fraude non autorisé.");
        }
    }

    @Caching(evict = {
        @CacheEvict(value = "settings",       allEntries = true),
        @CacheEvict(value = "settings-group", allEntries = true)
    })
    @Transactional(readOnly = true)
    public void reloadSettings() {
        log.info("AppSettings : cache vidé, rechargement depuis la DB");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TextEncryptor encryptor() {
        return Encryptors.text(encryptionKey, encryptionSalt);
    }

    private String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return plain;
        try { return encryptor().encrypt(plain); }
        catch (Exception e) { log.error("Erreur chiffrement setting: {}", e.getMessage()); return plain; }
    }

    private String decrypt(String cipher) {
        if (cipher == null || cipher.isBlank()) return cipher;
        try { return encryptor().decrypt(cipher); }
        catch (Exception e) {
            log.warn("Impossible de déchiffrer le setting — valeur retournée brute");
            return cipher;
        }
    }

    AppSettingDto toDto(AppSetting s) {
        AppSettingDto dto = new AppSettingDto();
        dto.setCle(s.getCle());
        dto.setValeur(s.isSensible() ? null : s.getValeur());   // jamais exposé en clair
        dto.setType(s.getType().name());
        dto.setGroupe(s.getGroupe().name());
        dto.setLabel(s.getLabel());
        dto.setDescription(s.getDescription());
        dto.setModifiableParAdmin(s.isModifiableParAdmin());
        dto.setSensible(s.isSensible());
        dto.setSensibleConfigured(s.isSensible() && s.getValeur() != null && !s.getValeur().isBlank());
        dto.setUpdatedAt(s.getUpdatedAt());
        dto.setUpdatedBy(s.getUpdatedBy());
        return dto;
    }
}
