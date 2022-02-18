/*
 * Copyright (c) Coveo Solutions Inc.
 */
package com.coveo.globalauroradatabase.datasource;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/*
 * This class is only needed to make the secondary datasource appeared in the metrics.
 * You can remove it if this is not needed for your use case.
*/
public class GlobalClusterRoutingDataSourceMetricsBinder implements MeterBinder
{
    private static final Logger logger = LoggerFactory.getLogger(GlobalClusterRoutingDataSourceMetricsBinder.class);

    private GlobalClusterRoutingDataSource globalClusterRoutingDataSource;

    public GlobalClusterRoutingDataSourceMetricsBinder(GlobalClusterRoutingDataSource globalClusterRoutingDataSource)
    {
        this.globalClusterRoutingDataSource = Objects.requireNonNull(globalClusterRoutingDataSource);
    }

    @Override
    public void bindTo(MeterRegistry registry)
    {
        /* The class org.springframework.boot.actuate.autoconfigure.metrics.jdbc.DataSourcePoolMetricsAutoConfiguration.HikariDataSourceMetricsConfiguration
         * doesn't pick up the secondary datasource so we wire it manually
         */
        @SuppressWarnings("resource")
        HikariDataSource secondaryHikariConnectionPool = globalClusterRoutingDataSource.getSecondaryDataSource();
        if (secondaryHikariConnectionPool.getMetricRegistry() == null
                && secondaryHikariConnectionPool.getMetricsTrackerFactory() == null) {
            try {
                secondaryHikariConnectionPool.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));
            } catch (Exception e) {
                logger.error("Failed to bind Hikari metrics for secondary datasource!", e);
            }
        }
    }
}
