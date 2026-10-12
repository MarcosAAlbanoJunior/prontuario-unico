package io.github.marcosaalbanojunior.prontuario.common.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.nio.file.Path;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app")
public record AppProperties(
        @NotBlank(message = "is required: set APP_PASSWORD_HASH")
        @Pattern(regexp = BCRYPT_HASH, message = "APP_PASSWORD_HASH must be a bcrypt hash")
        String passwordHash,
        @NotBlank(message = "is required: set APP_BASE_URL")
        @Pattern(regexp = HTTP_URL, message = "APP_BASE_URL must be an http(s) URL")
        String baseUrl,
        @NotNull ZoneId timezone,
        @NotNull Path dataDir) {

    static final String BCRYPT_HASH = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$";
    static final String HTTP_URL = "^https?://\\S+$";

    // Keeps the password hash out of logs and error messages.
    @Override
    public String toString() {
        return "AppProperties[passwordHash=***, baseUrl=%s, timezone=%s, dataDir=%s]"
                .formatted(baseUrl, timezone, dataDir);
    }
}
