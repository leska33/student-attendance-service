package com.example.student.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

/**
 * Spring Boot 4 binds {@code SPRING_DATASOURCE_URL} into {@code DataSourceProperties} before our
 * {@link EnvironmentPostProcessor} wins; supplying {@link JdbcConnectionDetails} is the supported override.
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@Conditional(RawPostgresEnvCondition.class)
public class RailwayJdbcConnectionDetailsConfiguration {

    @Bean
    JdbcConnectionDetails jdbcConnectionDetails(Environment environment) {
        PostgresRailwayDatasourceResolver.ResolvedDatasource r =
                PostgresRailwayDatasourceResolver.resolveForJdbcConnectionDetails(environment);
        String url = r.jdbcUrl();
        String user = r.username();
        String pass = r.password();
        return new JdbcConnectionDetails() {
            @Override
            public String getUsername() {
                return user;
            }

            @Override
            public String getPassword() {
                return pass;
            }

            @Override
            public String getJdbcUrl() {
                return url;
            }
        };
    }
}
