package com.example.student.config;

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
    private static final Pattern POSTGRES_URI =
            Pattern.compile("^postgres(?:ql)?://([^/:?#]+)(?::(\\d+))?/([^?]+)(?:\\?.*)?$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty(PROP_JDBC_URL) != null) {
            return;
        }

        String databaseUrl = environment.getProperty(PROP_DATABASE_URL);
        String jdbcFromDatabaseUrl = toJdbcUrl(databaseUrl);
        if (jdbcFromDatabaseUrl != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", jdbcFromDatabaseUrl);
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

    private String toJdbcUrl(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return null;
        }
        if (databaseUrl.startsWith("jdbc:postgresql://")) {
            return databaseUrl;
        }

        Matcher matcher = POSTGRES_URI.matcher(databaseUrl);
        if (!matcher.matches()) {
            return null;
        }

        String host = matcher.group(1);
        String port = matcher.group(2) != null ? matcher.group(2) : "5432";
        String database = matcher.group(3);
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }
}
