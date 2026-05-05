package com.example.student.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DatasourceEnvPostProcessor implements EnvironmentPostProcessor {

    private static final String PROP_JDBC_URL = "SPRING_DATASOURCE_URL";
    private static final String PROP_DATABASE_URL = "DATABASE_URL";
    private static final String PROP_HOST = "DB_HOST";
    private static final String PROP_PORT = "DB_PORT";
    private static final String PROP_DB = "DB_NAME";

    /**
     * Railway / Heroku style: postgresql://user:pass@host:5432/dbname?sslmode=require
     * Also supports host-only URLs without userinfo (legacy).
     */
    private static final Pattern POSTGRES_URI_WITH_AUTH = Pattern.compile(
            "^postgres(?:ql)?://"
                    + "(?:(?<user>[^:@/?#]+)(?::(?<pass>[^@/?#]*))?@)?"
                    + "(?<host>[^/:?#]+)"
                    + "(?::(?<port>\\d+))?/"
                    + "(?<db>[^?#]+)"
                    + "(?:\\?.*)?$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty(PROP_JDBC_URL) != null) {
            return;
        }

        String databaseUrl = environment.getProperty(PROP_DATABASE_URL);
        ParsedPostgresUri parsed = parsePostgresUri(databaseUrl);
        if (parsed != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", parsed.jdbcUrl());
            if (parsed.username() != null) {
                map.put("spring.datasource.username", parsed.username());
            }
            if (parsed.password() != null) {
                map.put("spring.datasource.password", parsed.password());
            }
            environment.getPropertySources().addFirst(new MapPropertySource("jdbcFromDatabaseUrl", map));
            return;
        }

        String host = environment.getProperty(PROP_HOST);
        if (host == null || host.isBlank()) {
            return;
        }

        String port = environment.getProperty(PROP_PORT, "5432");
        String db = environment.getProperty(PROP_DB, "studentdb");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        Map<String, Object> map = new HashMap<>();
        map.put("spring.datasource.url", url);
        environment.getPropertySources().addFirst(new MapPropertySource("jdbcFromHostPort", map));
    }

    private ParsedPostgresUri parsePostgresUri(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return null;
        }
        if (databaseUrl.startsWith("jdbc:postgresql://")) {
            return new ParsedPostgresUri(databaseUrl, null, null);
        }

        Matcher matcher = POSTGRES_URI_WITH_AUTH.matcher(databaseUrl.trim());
        if (!matcher.matches()) {
            return null;
        }

        String host = matcher.group("host");
        String port = matcher.group("port") != null ? matcher.group("port") : "5432";
        String database = matcher.group("db");
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        String user = matcher.group("user");
        String pass = matcher.group("pass");
        if (user == null) {
            return new ParsedPostgresUri(jdbcUrl, null, null);
        }
        return new ParsedPostgresUri(
                jdbcUrl,
                urlDecode(user),
                pass != null ? urlDecode(pass) : "");
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedPostgresUri(String jdbcUrl, String username, String password) {}
}
