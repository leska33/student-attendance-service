package com.example.student.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Normalizes Railway/Heroku {@code postgresql://...} URLs to {@code jdbc:postgresql://...} and
 * resolves credentials. Shared by {@link DatasourceEnvPostProcessor} and {@link RailwayJdbcConnectionDetailsConfiguration}.
 */
public final class PostgresRailwayDatasourceResolver {

    private static final Pattern POSTGRES_URI_HOST_ONLY = Pattern.compile(
            "^postgres(?:ql)?://"
                    + "(?<host>[^/:?#]+)"
                    + "(?::(?<port>\\d+))?/"
                    + "(?<db>[^?#]+)"
                    + "(?<query>\\?[^#]*)?$");

    private PostgresRailwayDatasourceResolver() {}

    public static boolean envHasRawPostgresUrl(Environment env) {
        return isRawPostgresUrl(env.getProperty("SPRING_DATASOURCE_URL"))
                || isRawPostgresUrl(env.getProperty("DATABASE_URL"));
    }

    private static boolean isRawPostgresUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String t = value.trim();
        return t.startsWith("postgres") && !t.startsWith("jdbc:");
    }

    /**
     * Used when {@link RailwayJdbcConnectionDetailsConfiguration} is active (raw {@code postgres://} in env).
     */
    public static ResolvedDatasource resolveForJdbcConnectionDetails(Environment env) {
        String raw = firstNonBlank(env.getProperty("SPRING_DATASOURCE_URL"), env.getProperty("DATABASE_URL"));
        Parsed parsed = parsePostgresUri(raw);
        if (parsed == null) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_URL or DATABASE_URL is set but could not be converted to a JDBC URL: " + raw);
        }
        return mergeCredentials(env, parsed);
    }

    /**
     * Host/port/db variables without {@code postgres://} URL (references to PGHOST etc.).
     */
    public static ResolvedDatasource resolveFromHostVariables(ConfigurableEnvironment env) {
        String host = env.getProperty("DB_HOST");
        if (host == null || host.isBlank()) {
            return null;
        }
        String port = env.getProperty("DB_PORT", "5432");
        String db = env.getProperty("DB_NAME", "studentdb");
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        return mergeCredentials(env, new Parsed(jdbcUrl, null, null));
    }

    private static ResolvedDatasource mergeCredentials(Environment env, Parsed parsed) {
        String username;
        if (hasExplicitUsername(env)) {
            username = firstNonBlankInOrder(
                    env.getProperty("SPRING_DATASOURCE_USERNAME"),
                    env.getProperty("PGUSER"),
                    env.getProperty("POSTGRES_USER"),
                    "postgres");
        } else {
            username = parsed.username != null
                    ? parsed.username
                    : firstNonBlankInOrder(
                            env.getProperty("SPRING_DATASOURCE_USERNAME"),
                            env.getProperty("PGUSER"),
                            env.getProperty("POSTGRES_USER"),
                            "postgres");
        }

        String password;
        if (hasExplicitPassword(env)) {
            password = firstNonBlankInOrder(
                    env.getProperty("SPRING_DATASOURCE_PASSWORD"),
                    env.getProperty("PGPASSWORD"),
                    env.getProperty("DB_PASSWORD"),
                    env.getProperty("POSTGRES_PASSWORD"),
                    "");
        } else {
            password = parsed.password != null
                    ? parsed.password
                    : firstNonBlankInOrder(
                            env.getProperty("SPRING_DATASOURCE_PASSWORD"),
                            env.getProperty("PGPASSWORD"),
                            env.getProperty("DB_PASSWORD"),
                            env.getProperty("POSTGRES_PASSWORD"),
                            "postgres");
        }

        return new ResolvedDatasource(parsed.jdbcUrl, username, password);
    }

    private static boolean hasExplicitUsername(Environment env) {
        return env.getProperty("SPRING_DATASOURCE_USERNAME") != null
                || env.getProperty("PGUSER") != null
                || env.getProperty("POSTGRES_USER") != null;
    }

    private static boolean hasExplicitPassword(Environment env) {
        return env.getProperty("SPRING_DATASOURCE_PASSWORD") != null
                || env.getProperty("PGPASSWORD") != null
                || env.getProperty("DB_PASSWORD") != null
                || env.getProperty("POSTGRES_PASSWORD") != null;
    }

    private static String firstNonBlank(String a, String b) {
        return firstNonBlankInOrder(a, b);
    }

    /** First non-blank; if {@code fallback} is last and all prior are blank, returns {@code fallback} (may be blank). */
    private static String firstNonBlankInOrder(String... parts) {
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                return p;
            }
        }
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    private static Parsed parsePostgresUri(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return null;
        }
        String trimmed = databaseUrl.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return new Parsed(trimmed, null, null);
        }

        Parsed fromUri = parsePostgresWithJavaUri(trimmed);
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
        return new Parsed(jdbcUrl, null, null);
    }

    private static Parsed parsePostgresWithJavaUri(String trimmed) {
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
            return new Parsed(jdbcUrl, null, null);
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
        return new Parsed(jdbcUrl, user, pass);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static final class Parsed {
        final String jdbcUrl;
        final String username;
        final String password;

        Parsed(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }

    public record ResolvedDatasource(String jdbcUrl, String username, String password) {}
}
