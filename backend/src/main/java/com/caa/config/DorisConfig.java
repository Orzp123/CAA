package com.caa.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Secondary (read-only) datasource for Apache Doris analytics queries.
 * The primary datasource is auto-configured by Spring Boot from spring.datasource.*.
 */
@Configuration
public class DorisConfig {

    @Bean(name = "dorisHikariConfig")
    @ConfigurationProperties(prefix = "doris.datasource.hikari")
    public HikariConfig dorisHikariConfig() {
        return new HikariConfig();
    }

    @Bean(name = "dorisDataSource")
    public DataSource dorisDataSource(
            @Qualifier("dorisHikariConfig") HikariConfig hikariConfig,
            @org.springframework.beans.factory.annotation.Value("${doris.datasource.url}") String url,
            @org.springframework.beans.factory.annotation.Value("${doris.datasource.username}") String username,
            @org.springframework.beans.factory.annotation.Value("${doris.datasource.password}") String password,
            @org.springframework.beans.factory.annotation.Value("${doris.datasource.driver-class-name}") String driverClassName
    ) {
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setReadOnly(true);
        hikariConfig.setPoolName("DorisHikariPool");
        return new HikariDataSource(hikariConfig);
    }

    @Bean(name = "dorisJdbcTemplate")
    public JdbcTemplate dorisJdbcTemplate(@Qualifier("dorisDataSource") DataSource dorisDataSource) {
        return new JdbcTemplate(dorisDataSource);
    }
}
