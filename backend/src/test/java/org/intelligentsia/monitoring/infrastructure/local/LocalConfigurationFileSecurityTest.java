package org.intelligentsia.monitoring.infrastructure.local;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import java.nio.file.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

/** Validate the authorized migration without logging secrets or sending email. */
class LocalConfigurationFileSecurityTest {
    @Test void accountsAreHashedSmtpRemainsEncryptedAndNoBusinessDataOrSessionsAreSaved() throws Exception {
        JsonNode state=new ObjectMapper().readTree(Path.of("local-config/application-state.json").toFile());
        for (String table:new String[]{"institutions","atms","monitoring_alerts","fraud_detections","refresh_tokens"}) assertFalse(state.has(table),"Excluded table present: "+table);
        for (JsonNode user:state.path("users")) assertTrue(user.path("password_hash").asText().startsWith("$2"),"A local account must retain a password hash.");
        String yaml=Files.readString(Path.of("src/main/resources/application.yml"));
        String key=value(yaml,"SETTINGS_ENCRYPTION_KEY"),salt=value(yaml,"SETTINGS_ENCRYPTION_SALT");
        var encryptor=Encryptors.text(key,salt);
        for (JsonNode setting:state.path("app_settings")) if (setting.path("sensible").asBoolean() && !setting.path("valeur").asText().isBlank()) {
            String encrypted=setting.path("valeur").asText();
            assertTrue(encrypted.matches("[0-9a-fA-F]+"),"Sensitive local values must remain encrypted.");
            assertDoesNotThrow(() -> encryptor.decrypt(encrypted),"The authorized encrypted setting must remain usable.");
        }
    }
    private String value(String yaml,String name) {
        String configured=System.getenv(name);if(configured!=null)return configured;
        Matcher match=Pattern.compile("\\$\\{"+name+":([^}]+)\\}").matcher(yaml);
        if(!match.find())throw new IllegalStateException("Encryption configuration missing");return match.group(1);
    }
}
