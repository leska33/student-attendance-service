package com.example.student.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Railway/Heroku expose {@code DATABASE_URL} / {@code SPRING_DATASOURCE_URL} as {@code postgresql://...}.
 * HikariCP requires {@code jdbc:postgresql://...}. Also, relaxed binding can feed the non-JDBC URL into
 * {@code spring.datasource.url} before normalization — so {@code application.properties} must use
 * {@code DEPLOY_DATASOURCE_JDBC_URL} (set only here) instead of referencing those env vars directly.
 */
public class DatasourceEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Populated only by this processor; referenced from {@code application.properties}. */
    public static final String PROP_DEPLOY_JDBC_URL = "DEPLOY_DATASOURCE_JDBC_URL";

    private static final String PROP_JDBC_URL = "SPRING_DATASOURCE_URL";
    private static final String PROP_DATABASE_URL = "DATABASE_URL";
    private static final String PROP_HOST = "DB_HOST";
    private static final String PROP_PORT = "DB_PORT";
    private static final String PROP_DB = "DB_NAME";

    /**
     * Host-only URLs without userinfo (legacy).
     */
    private static final Pattern POSTGRES_URI_HOST_ONLY = Pattern.compile(
            "^postgres(?:ql)?://"
                    + "(?<host>[^/:?#]+)"
                    + "(?::(?<port>\\d+))?/"
                    + "(?<db>[^?#]+)"
                    + "(?<query>\\?[^#]*)?$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> map = new HashMap<>();

        String springDsUrl = environment.getProperty(PROP_JDBC_URL);
        String databaseUrl = environment.getProperty(PROP_DATABASE_URL);
        String rawUrl = firstNonBlank(springDsUrl, databaseUrl);

        ParsedPostgresUri parsed = parsePostgresUri(rawUrl);
        if (parsed != null) {
            putJdbcUrl(map, parsed.jdbcUrl());
            if (parsed.username() != null && !hasExplicitUsername(environment)) {
                map.put("spring.datasource.username", parsed.username());
            }
            if (parsed.password() != null && !hasExplicitPassword(environment)) {
                map.put("spring.datasource.password", parsed.password());
            }
            environment.getPropertySources().addFirst(new MapPropertySource("deployDatasource", map));
            return;
        }

        String host = environment.getProperty(PROP_HOST);
        if (host == null || host.isBlank()) {
            return;
        }

        String port = environment.getProperty(PROP_PORT, "5432");
        String db = environment.getProperty(PROP_DB, "studentdb");
        putJdbcUrl(map, "jdbc:postgresql://" + host + ":" + port + "/" + db);
        environment.getPropertySources().addFirst(new MapPropertySource("deployDatasourceHostPort", map));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /** Sets both keys so env {@code SPRING_DATASOURCE_URL} cannot win over a non-{@code jdbc:} URL. */
    private static void putJdbcUrl(Map<String, Object> map, String jdbcUrl) {
        map.put(PROP_DEPLOY_JDBC_URL, jdbcUrl);
        map.put("spring.datasource.url", jdbcUrl);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static boolean hasExplicitUsername(ConfigurableEnvironment environment) {
        return environment.getProperty("SPRING_DATASOURCE_USERNAME") != null
                || environment.getProperty("PGUSER") != null
                || environment.getProperty("POSTGRES_USER") != null;
    }

    private static boolean hasExplicitPassword(ConfigurableEnvironment environment) {
        return environment.getProperty("SPRING_DATASOURCE_PASSWORD") != null
                || environment.getProperty("PGPASSWORD") != null
                || environment.getProperty("DB_PASSWORD") != null
                || environment.getProperty("POSTGRES_PASSWORD") != null;
    }

    private ParsedPostgresUri parsePostgresUri(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return null;
        }
        String trimmed = databaseUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return new ParsedPostgresUri(trimmed, null, null);
        }

        ParsedPostgresUri fromUri = parsePostgresWithJavaUri(trimmed);
        if (fromUri != null) {
            return fromUri;
        }

        Matcher matcher = POSTGRES_URI_HOST_ONLY.matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }

        String host = matcher.group("host");
        String port = matcher.group("port") != null ? matcher.group("port") : "5432";
        String database = matcher.group("db");
        String query = matcher.group("query");
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database
                + (query != null ? query : "");
        return new ParsedPostgresUri(jdbcUrl, null, null);
    }

    /**
     * Handles user:password@host (including encoded characters in password) better than a single regex.
     */
    private ParsedPostgresUri parsePostgresWithJavaUri(String trimmed) {
        if (!trimmed.startsWith("postgres")) {
            return null;
        }
        int schemeEnd = trimmed.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        String rest = trimmed.substring(schemeEnd + 3);
        URI uri;
        try {
            uri = URI.create("http://" + rest);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return null;
        }
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String rawPath = uri.getRawPath();
        if (rawPath == null || rawPath.length() <= 1) {
            return null;
        }
        String database = rawPath.substring(1);
        String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database + query;

        String rawUserInfo = uri.getRawUserInfo();
        if (rawUserInfo == null || rawUserInfo.isEmpty()) {
            return new ParsedPostgresUri(jdbcUrl, null, null);
        }
        String user;
        String pass;
        int colon = rawUserInfo.indexOf(':');
        if (colon >= 0) {
            user = urlDecode(rawUserInfo.substring(0, colon));
            pass = urlDecode(rawUserInfo.substring(colon + 1));
        } else {
            user = urlDecode(rawUserInfo);
            pass = "";
        }
        return new ParsedPostgresUri(jdbcUrl, user, pass);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedPostgresUri(String jdbcUrl, String username, String password) {}
}
