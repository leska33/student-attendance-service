package com.example.student.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DatasourceEnvPostProcessor implements EnvironmentPostProcessor {

    private static final String PROP_JDBC_URL = "SPRING_DATASOURCE_URL";
    private static final String PROP_HOST = "DB_HOST";
    private static final String PROP_PORT = "DB_PORT";
    private static final String PROP_DB = "DB_NAME";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty(PROP_JDBC_URL) != null || environment.getProperty(PROP_HOST) == null) {
            return;
        }
        String host = environment.getProperty(PROP_HOST);
        String port = environment.getProperty(PROP_PORT, "5432");
        String db = environment.getProperty(PROP_DB, "studentdb");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        Map<String, Object> map = new HashMap<>();
        map.put("spring.datasource.url", url);
        environment.getPropertySources().addFirst(new MapPropertySource("jdbcFromHostPort", map));
    }
}
