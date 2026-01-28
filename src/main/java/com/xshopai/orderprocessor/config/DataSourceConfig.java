package com.xshopai.orderprocessor.config;

import com.xshopai.orderprocessor.client.DaprSecretManager;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * DataSource configuration with multi-source support
 * 
 * Configuration sources (in order of precedence):
 * 1. Spring properties (SPRING_DATASOURCE_URL, etc.) - for direct ACA env vars
 * 2. Dapr Secret Manager - for Dapr secret store integration
 * 
 * This allows the service to work both:
 * - Locally with Dapr file-based secret store
 * - On Azure with direct environment variables or Key Vault
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSourceConfig {

    private final DaprSecretManager secretManager;
    private final Environment environment;

    @Bean
    public DataSource dataSource() {
        log.info("Configuring DataSource...");
        
        // Check for Spring standard properties first (set via environment variables)
        String springUrl = environment.getProperty("spring.datasource.url");
        String springUsername = environment.getProperty("spring.datasource.username");
        String springPassword = environment.getProperty("spring.datasource.password");
        
        if (springUrl != null && !springUrl.isEmpty()) {
            log.info("Using Spring DataSource properties from environment");
            log.info("Database URL: {}", springUrl.replaceAll("password=[^&]*", "password=***"));
            
            return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(springUrl)
                .username(springUsername)
                .password(springPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
        }
        
        // Fall back to Dapr Secret Manager
        log.info("Using Dapr Secret Manager for database configuration...");
        DaprSecretManager.DatabaseConfig dbConfig = secretManager.getDatabaseConfig();
        
        if (dbConfig == null || dbConfig.host() == null) {
            throw new IllegalStateException(
                "Database configuration not found. Set SPRING_DATASOURCE_URL environment variable " +
                "or configure database:host, database:port, database:name, database:user, database:password in Dapr secret store"
            );
        }
        
        log.info("Database configuration loaded: host={}, port={}, database={}", 
            dbConfig.host(), dbConfig.port(), dbConfig.name());
        
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .url(dbConfig.getJdbcUrl())
            .username(dbConfig.user())
            .password(dbConfig.password())
            .driverClassName("org.postgresql.Driver")
            .build();
    }
}
