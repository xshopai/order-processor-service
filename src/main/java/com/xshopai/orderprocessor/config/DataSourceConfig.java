package com.xshopai.orderprocessor.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * DataSource configuration using environment variables
 * 
 * Configuration via Spring properties (set via environment variables):
 * - SPRING_DATASOURCE_URL
 * - SPRING_DATASOURCE_USERNAME
 * - SPRING_DATASOURCE_PASSWORD
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSourceConfig {

    private final Environment environment;

    @Bean
    public DataSource dataSource() {
        log.info("Configuring DataSource...");
        
        // Get Spring standard properties from environment variables
        String springUrl = environment.getProperty("spring.datasource.url");
        String springUsername = environment.getProperty("spring.datasource.username");
        String springPassword = environment.getProperty("spring.datasource.password");
        
        if (springUrl == null || springUrl.isEmpty()) {
            throw new IllegalStateException(
                "Database configuration not found. Set SPRING_DATASOURCE_URL environment variable"
            );
        }
        
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
}
