package com.passwordleakdetector.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlSupportTest {

    @Test
    void splitsConnectionStringIntoDatasourceProperties() {
        Map<String, String> props = DatabaseUrlSupport.toDatasourceProperties(
                "postgresql://app:s3cret@dbhost:5432/password_leak_detector");

        assertThat(props)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://dbhost:5432/password_leak_detector")
                .containsEntry("spring.datasource.username", "app")
                .containsEntry("spring.datasource.password", "s3cret");
    }

    @Test
    void defaultsToStandardPostgresPortWhenMissing() {
        Map<String, String> props = DatabaseUrlSupport.toDatasourceProperties(
                "postgresql://app:s3cret@dbhost/password_leak_detector");

        assertThat(props.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://dbhost:5432/password_leak_detector");
    }

    @Test
    void returnsEmptyMapWhenDatabaseUrlIsAbsent() {
        assertThat(DatabaseUrlSupport.toDatasourceProperties(null)).isEmpty();
        assertThat(DatabaseUrlSupport.toDatasourceProperties("")).isEmpty();
        assertThat(DatabaseUrlSupport.toDatasourceProperties("   ")).isEmpty();
    }

    @Test
    void rejectsMalformedUri() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> DatabaseUrlSupport.toDatasourceProperties("not a valid uri :://"));
    }
}
