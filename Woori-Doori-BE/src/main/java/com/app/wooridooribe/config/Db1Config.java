package com.app.wooridooribe.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Objects;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.app.wooridooribe.repository",
        entityManagerFactoryRef = "db1EntityManagerFactory",
        transactionManagerRef = "db1TransactionManager"
)
public class Db1Config {

    @Primary
    @Bean(name = "db1DataSource")
    @ConfigurationProperties("spring.datasource.db1")
    public DataSource db1DataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Primary
    @Bean(name = "db1EntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean db1EntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("db1DataSource") DataSource dataSource
    ) {
        return builder
                .dataSource(dataSource)
                .packages("com.app.wooridooribe.entity")
                .persistenceUnit("db1")
                .build();
    }

    @Primary
    @Bean(name = "db1TransactionManager")
    public PlatformTransactionManager db1TransactionManager(
            @Qualifier("db1EntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory));
    }
}

