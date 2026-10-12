package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.support.TestcontainersConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** A JDBC slice against PostgreSQL 18 in Testcontainers, with Flyway applied; each test rolls back. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DisabledInNativeImage
@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public @interface PostgresTest {}
