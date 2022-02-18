/*
 * Copyright (c) Coveo Solutions Inc.
 */
package com.coveo.globalauroradatabase.configuration;

import javax.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.coveo.globalauroradatabase.datasource.GlobalClusterAwareTransactionManager;
import com.coveo.globalauroradatabase.datasource.GlobalClusterRoutingDataSource;
import com.coveo.globalauroradatabase.datasource.GlobalClusterRoutingDataSourceMetricsBinder;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class GlobalClusterDataSourceConfiguration
{
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    @Primary
    public DataSourceProperties primaryDataSourceProperties()
    {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSourceProperties secondaryDataSourceProperties()
    {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public GlobalClusterRoutingDataSource routingDataSource(DataSourceProperties primaryDataSourceProperties,
                                                            DataSourceProperties secondaryDataSourceProperties)
    {
        HikariDataSource primaryHikariDataSource = primaryDataSourceProperties.initializeDataSourceBuilder()
                                                                              .type(HikariDataSource.class)
                                                                              .build();
        primaryHikariDataSource.setPoolName("Primary-HikariPool");

        HikariDataSource secondaryHikariDataSource = secondaryDataSourceProperties.initializeDataSourceBuilder()
                                                                                  .type(HikariDataSource.class)
                                                                                  .build();
        secondaryHikariDataSource.setPoolName("Secondary-HikariPool");

        return new GlobalClusterRoutingDataSource(primaryHikariDataSource, secondaryHikariDataSource);
    }

    @Bean
    public GlobalClusterRoutingDataSourceMetricsBinder globalClusterRoutingDataSourceMetricsConfiguration(GlobalClusterRoutingDataSource routingDataSource)
    {
        return new GlobalClusterRoutingDataSourceMetricsBinder(routingDataSource);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                       GlobalClusterRoutingDataSource routingDataSource)
    {
        return builder.dataSource(routingDataSource).packages("com.coveo").build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(PlatformTransactionManager wrappedPlatformTransactionManager)
    {
        return new GlobalClusterAwareTransactionManager(wrappedPlatformTransactionManager);
    }

    @Bean
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory emf)
    {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public boolean demoBean(GlobalClusterRoutingDataSource routingDataSource) throws Exception
    {
        // This is just a small bean to create an initial connection on the secondary data source and make it appear
        // in the metrics. You should not need this to make your application work.
        routingDataSource.getSecondaryDataSource().getConnection();
        return true;
    }
}
