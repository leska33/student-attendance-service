package com.example.student.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Publishes {@link #PROP_DEPLOY_JDBC_URL} when only host/port variables are set. Raw {@code postgresql://}
 * URLs are handled by {@link RailwayJdbcConnectionDetailsConfiguration} (Spring Boot 4 binds env
 * {@code SPRING_DATASOURCE_URL} too early for this post-processor alone to win).
 */
public class DatasourceEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROP_DEPLOY_JDBC_URL = "DEPLOY_DATASOURCE_JDBC_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (PostgresRailwayDatasourceResolver.envHasRawPostgresUrl(environment)) {
            return;
        }

        PostgresRailwayDatasourceResolver.ResolvedDatasource resolved =
                PostgresRailwayDatasourceResolver.resolveFromHostVariables(environment);
        if (resolved == null) {
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put(PROP_DEPLOY_JDBC_URL, resolved.jdbcUrl());
        map.put("spring.datasource.url", resolved.jdbcUrl());
        if (!hasExplicitUsername(environment)) {
            map.put("spring.datasource.username", resolved.username());
        }
        if (!hasExplicitPassword(environment)) {
            map.put("spring.datasource.password", resolved.password());
        }
        environment.getPropertySources().addFirst(new MapPropertySource("deployDatasourceHostPort", map));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
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
}
