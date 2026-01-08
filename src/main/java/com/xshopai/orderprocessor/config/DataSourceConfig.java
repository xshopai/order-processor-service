package com.xshopai.orderprocessor.config;

import com.xshopai.orderprocessor.client.DaprSecretManager;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * DataSource configuration using Dapr Secret Manager
 * Retrieves database credentials from Dapr secret store at runtime
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSourceConfig {

    private final DaprSecretManager secretManager;

    @Bean
    public DataSource dataSource() {
        log.info("Configuring DataSource using Dapr secrets...");
        
        DaprSecretManager.DatabaseConfig dbConfig = secretManager.getDatabaseConfig();
        
        if (dbConfig == null || dbConfig.host() == null) {
            throw new IllegalStateException("Database configuration not found in Dapr secrets");
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
