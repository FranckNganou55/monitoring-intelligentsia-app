package org.intelligentsia.monitoring.domain.configuration.settings.controller;

import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.settings.dto.AppSettingDto;
import org.intelligentsia.monitoring.domain.configuration.settings.dto.AppSettingUpdateRequest;
import org.intelligentsia.monitoring.domain.configuration.settings.dto.FraudAlertCadenceRequest;
import org.intelligentsia.monitoring.domain.configuration.settings.dto.TestMailRequest;
import org.intelligentsia.monitoring.domain.configuration.settings.entity.SettingGroupe;
import org.intelligentsia.monitoring.domain.configuration.settings.service.AppSettingService;
import org.intelligentsia.monitoring.domain.configuration.settings.service.MailConfigService;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class AppSettingController {

    private final AppSettingService settingService;
    private final MailConfigService mailConfig;

    @GetMapping("/alert-sound-cadence")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getAlertSoundCadence() {
        return ResponseEntity.ok(ApiResponse.ok(settingService.getAlertSoundCadence()));
    }

    @GetMapping("/fraud-alert-cadence")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getFraudAlertCadence() {
        return ResponseEntity.ok(ApiResponse.ok(settingService.getFraudAlertCadence()));
    }

    @GetMapping("/{groupe}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AppSettingDto>>> getByGroupe(
            @PathVariable String groupe) {
        SettingGroupe g = SettingGroupe.valueOf(groupe.toUpperCase());
        return ResponseEntity.ok(ApiResponse.ok(settingService.getByGroupe(g)));
    }

    @PutMapping("/{cle}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AppSettingDto>> update(
            @PathVariable String cle,
            @Valid @RequestBody AppSettingUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(settingService.update(cle, req)));
    }

    @PutMapping("/fraud-alert-cadence")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateFraudAlertCadence(
            @Valid @RequestBody FraudAlertCadenceRequest request) {
        settingService.updateFraudAlertCadence(request);
        return ResponseEntity.ok(ApiResponse.ok("Fréquences des notifications fraude appliquées."));
    }

    @PutMapping("/atm-interface/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateAtmInterface(@PathVariable String status,
            @Valid @RequestBody org.intelligentsia.monitoring.domain.configuration.settings.dto.AtmInterfaceSettingsRequest request) {
        settingService.updateAtmInterfaceSettings(status, request);
        return ResponseEntity.ok(ApiResponse.ok("Paramètres " + status + " appliqués."));
    }

    @PostMapping("/reload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reload() {
        settingService.reloadSettings();
        return ResponseEntity.ok(ApiResponse.ok("Cache des paramètres vidé et rechargé."));
    }

    @PostMapping("/test-mail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> testMail(
            @Valid @RequestBody TestMailRequest req) {
        try {
            JavaMailSender sender = mailConfig.buildSender();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(mailConfig.getFromAddress(), mailConfig.getFromName());
            helper.setTo(req.getTo());
            helper.setSubject("Test email — Monitoring Monétique");
            helper.setText(
                "<p>Ceci est un <b>email de test</b> envoyé depuis la page Paramètres.</p>"
                + "<p>Si vous recevez ce message, la configuration SMTP est correcte.</p>",
                true);
            sender.send(msg);
            log.info("Email de test envoyé à {}", req.getTo());
            return ResponseEntity.ok(ApiResponse.ok("Email envoyé avec succès à " + req.getTo()));
        } catch (Exception e) {
            log.error("Échec email de test: {}", e.getMessage());
            return ResponseEntity.ok(
                ApiResponse.<String>error("MAIL_TEST_FAILED", "Échec : " + e.getMessage()));
        }
    }
}
