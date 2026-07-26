package com.passwordleakdetector.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Some hosting platforms (Render, Heroku, Railway...) hand a managed Postgres
 * database to the app as a single connection-string env var in the form
 * {@code postgresql://user:password@host:port/database} rather than as
 * separate JDBC properties. Spring's {@code spring.datasource.url} requires a
 * literal {@code jdbc:postgresql://...} URL, so this splits DATABASE_URL into
 * the three datasource properties Spring expects.
 *
 * <p>This is applied via {@code System.setProperty} at the very start of
 * {@code main()} rather than as a {@code EnvironmentPostProcessor} - when
 * packaged in the same fat jar as the application itself (not a separate
 * library jar under BOOT-INF/lib), the {@code META-INF/spring.factories} /
 * {@code *.imports} registration files get relocated to the outer jar's root
 * during Spring Boot repackaging, which the app's own launched classloader
 * never scans, so that SPI-based hook silently never fires.
 */
public final class DatabaseUrlSupport {

    private DatabaseUrlSupport() {
    }

    public static Map<String, String> toDatasourceProperties(String databaseUrl) {
        Map<String, String> properties = new LinkedHashMap<>();
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return properties;
        }

        URI uri;
        try {
            uri = new URI(databaseUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("DATABASE_URL is not a valid URI: " + e.getMessage(), e);
        }

        properties.put("spring.datasource.url",
                "jdbc:postgresql://" + uri.getHost() + ":" + (uri.getPort() > 0 ? uri.getPort() : 5432) + uri.getPath());

        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            int separator = userInfo.indexOf(':');
            String username = separator >= 0 ? userInfo.substring(0, separator) : userInfo;
            String password = separator >= 0 ? userInfo.substring(separator + 1) : "";
            properties.put("spring.datasource.username", username);
            properties.put("spring.datasource.password", password);
        }

        return properties;
    }
}
