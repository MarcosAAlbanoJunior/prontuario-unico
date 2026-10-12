package io.github.marcosaalbanojunior.prontuario.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.telegram.config.TelegramProperties;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
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
class RequiredSettingsTest {

    private static final Map<String, String> VALID = Map.of(
            "APP_PASSWORD_HASH", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy",
            "APP_BASE_URL", "https://saude.example.com",
            "TELEGRAM_BOT_TOKEN", "123:abc",
            "TELEGRAM_ALLOWED_CHAT_ID", "42");

    @Test
    void shouldStartWithRequiredSettingsAndApplyDefaults() {
        runnerWith(VALID).run(context -> {
            assertThat(context).hasNotFailed();
            var app = context.getBean(AppProperties.class);
            assertThat(app.timezone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
            assertThat(app.dataDir()).hasToString("/data");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"APP_PASSWORD_HASH", "APP_BASE_URL", "TELEGRAM_BOT_TOKEN", "TELEGRAM_ALLOWED_CHAT_ID"})
    void shouldRefuseToStartWhenRequiredSettingIsMissing(String missing) {
        var settings = new HashMap<>(VALID);
        settings.remove(missing);

        runnerWith(settings).run(context -> assertThat(context)
                .getFailure()
                .hasStackTraceContaining(missing));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "APP_PASSWORD_HASH        | ''",
        "APP_PASSWORD_HASH        | plain-password",
        "APP_BASE_URL             | ''",
        "APP_BASE_URL             | saude.example.com",
        "TELEGRAM_BOT_TOKEN       | ''",
        "TELEGRAM_ALLOWED_CHAT_ID | not-a-number",
    })
    void shouldRefuseToStartWhenRequiredSettingIsInvalid(String name, String value) {
        var settings = new HashMap<>(VALID);
        settings.put(name, value);

        runnerWith(settings).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldNotRevealSecretsWhenPrinted() {
        runnerWith(VALID).run(context -> {
            assertThat(context.getBean(AppProperties.class).toString()).doesNotContain(VALID.get("APP_PASSWORD_HASH"));
            assertThat(context.getBean(TelegramProperties.class).toString()).doesNotContain(VALID.get("TELEGRAM_BOT_TOKEN"));
        });
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
