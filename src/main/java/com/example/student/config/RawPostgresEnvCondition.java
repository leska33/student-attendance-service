package com.example.student.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Enables {@link RailwayJdbcConnectionDetailsConfiguration} when Railway-style {@code postgresql://}
 * URLs are present (Hikari otherwise sees a non-{@code jdbc:} URL via {@code DataSourceProperties}).
 */
public class RawPostgresEnvCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return PostgresRailwayDatasourceResolver.envHasRawPostgresUrl(context.getEnvironment());
    }
}
