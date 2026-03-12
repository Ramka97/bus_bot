package com.busfleet.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Если в окружении задана переменная DATABASE_URL (Railway при добавлении PostgreSQL),
 * подставляет из неё настройки DataSource и отключает H2 Console.
 */
public class RailwayDatabaseUrlProcessor implements EnvironmentPostProcessor {

    private static final String DATABASE_URL = "DATABASE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = System.getenv(DATABASE_URL);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        try {
            if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
                return;
            }

            // Разделяем по последнему @ (пароль может содержать @)
            int lastAt = databaseUrl.lastIndexOf('@');
            if (lastAt <= 0) return;
            String userInfo = databaseUrl.substring(databaseUrl.indexOf("://") + 3, lastAt);
            String hostPortDb = databaseUrl.substring(lastAt + 1);

            int firstColon = userInfo.indexOf(':');
            String username = firstColon >= 0 ? decode(userInfo.substring(0, firstColon)) : decode(userInfo);
            String password = firstColon >= 0 ? decode(userInfo.substring(firstColon + 1)) : "";

            // host:port/database или host/database
            int slash = hostPortDb.indexOf('/');
            String hostPort = slash >= 0 ? hostPortDb.substring(0, slash) : hostPortDb;
            String database = slash >= 0 ? hostPortDb.substring(slash + 1).split("\\?")[0] : "railway";
            if (database.isEmpty()) database = "railway";

            int lastColon = hostPort.lastIndexOf(':');
            String host = lastColon >= 0 ? hostPort.substring(0, lastColon) : hostPort;
            int port = lastColon >= 0 ? Integer.parseInt(hostPort.substring(lastColon + 1)) : 5432;

            String jdbcUrlWithHost = "jdbc:postgresql://" + host + ":" + port + "/" + database;

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbcUrlWithHost);
            props.put("spring.datasource.username", username != null ? username : "");
            props.put("spring.datasource.password", password != null ? password : "");
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("spring.h2.console.enabled", "false");

            environment.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseUrl", props));
        } catch (Exception e) {
            System.err.println("RailwayDatabaseUrlProcessor: не удалось разобрать DATABASE_URL: " + e.getMessage());
        }
    }

    private static String decode(String s) {
        if (s == null) return "";
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
