package io.github.marcosaalbanojunior.prontuario.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

// ApplicationContextRunner builds contexts at runtime, which AOT cannot process.
@DisabledInNativeImage
@DisplayName("Required settings (AppProperties, TelegramProperties)")
class RequiredSettingsTest {

    private static final Map<String, String> VALID = Map.of(
            "APP_PASSWORD_HASH", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy",
            "APP_BASE_URL", "https://saude.example.com",
            "TELEGRAM_BOT_TOKEN", "123:abc",
            "TELEGRAM_ALLOWED_CHAT_ID", "42");

    @Nested
    @DisplayName("with every required setting")
    class Valid {

        @Test
        @DisplayName("starts and applies the defaults")
        void should_start_when_requiredSettingsArePresent() {
            runnerWith(VALID).run(context -> {
                assertThat(context).hasNotFailed();
                var app = context.getBean(AppProperties.class);
                var telegram = context.getBean(TelegramProperties.class);
                assertThat(app.timezone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
                assertThat(app.dataDir()).hasToString("/data");
                assertThat(telegram.apiUrl()).isEqualTo("https://api.telegram.org");
                assertThat(telegram.polling()).isTrue();
            });
        }

        @Test
        @DisplayName("never prints secrets")
        void should_hideSecrets_when_printed() {
            runnerWith(VALID).run(context -> {
                assertThat(context.getBean(AppProperties.class).toString()).doesNotContain(VALID.get("APP_PASSWORD_HASH"));
                assertThat(context.getBean(TelegramProperties.class).toString()).doesNotContain(VALID.get("TELEGRAM_BOT_TOKEN"));
            });
        }
    }

    @Nested
    @DisplayName("with a missing or invalid setting")
    class Invalid {

        @ParameterizedTest(name = "refuses to start without {0}")
        @ValueSource(strings = {"APP_PASSWORD_HASH", "APP_BASE_URL", "TELEGRAM_BOT_TOKEN", "TELEGRAM_ALLOWED_CHAT_ID"})
        @DisplayName("refuses to start and names the missing variable")
        void should_refuseToStart_when_requiredSettingIsMissing(String missing) {
            var settings = new HashMap<>(VALID);
            settings.remove(missing);

            runnerWith(settings).run(context -> assertThat(context).getFailure().hasStackTraceContaining(missing));
        }

        @ParameterizedTest(name = "refuses {0}=\"{1}\"")
        @CsvSource(delimiter = '|', value = {
            "APP_PASSWORD_HASH        | ''",
            "APP_PASSWORD_HASH        | plain-password",
            "APP_BASE_URL             | ''",
            "APP_BASE_URL             | saude.example.com",
            "TELEGRAM_BOT_TOKEN       | ''",
            "TELEGRAM_ALLOWED_CHAT_ID | not-a-number",
        })
        @DisplayName("refuses to start with an invalid value")
        void should_refuseToStart_when_requiredSettingIsInvalid(String name, String value) {
            var settings = new HashMap<>(VALID);
            settings.put(name, value);

            runnerWith(settings).run(context -> assertThat(context).hasFailed());
        }
    }

    private static ApplicationContextRunner runnerWith(Map<String, String> settings) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(SettingsConfig.class)
                .withPropertyValues(settings.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .toArray(String[]::new));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AppProperties.class, TelegramProperties.class})
    static class SettingsConfig {}
}
