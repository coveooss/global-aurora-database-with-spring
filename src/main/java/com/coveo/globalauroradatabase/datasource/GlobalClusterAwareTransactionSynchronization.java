/*
 * Copyright (c) Coveo Solutions Inc.
 */
package com.coveo.globalauroradatabase.datasource;

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class GlobalClusterAwareTransactionSynchronization implements TransactionSynchronization
{
    public static final String GLOBAL_CLUSTER_DATA_SOURCE_TYPE_KEY = GlobalClusterAwareTransactionManager.class.getName()
            + ".dataSourceType";

    private Deque<GlobalClusterRoutingDataSource.DataSourceType> previousDataSourceTypes;
    private GlobalClusterRoutingDataSource.DataSourceType currentTransactionDataSourceType;

    public GlobalClusterAwareTransactionSynchronization()
    {
        this.previousDataSourceTypes = new ArrayDeque<>();
    }

    @Override
    public void suspend()
    {
        previousDataSourceTypes.push(getCurrentDataSourceType());
        cleanupDataSourceType();
        setDataSourceType(currentTransactionDataSourceType);
    }

    @Override
    public void resume()
    {
        GlobalClusterRoutingDataSource.DataSourceType previousDatasourceType = previousDataSourceTypes.pop();
        setDataSourceType(previousDatasourceType);
    }

    @Override
    public void afterCompletion(int status)
    {
        cleanupDataSourceType();
    }

    public void setCurrentTransactionDataSourceType(boolean isReadonly)
    {
        this.currentTransactionDataSourceType = isReadonly ? GlobalClusterRoutingDataSource.DataSourceType.SECONDARY
                                                           : GlobalClusterRoutingDataSource.DataSourceType.PRIMARY;
    }

    public GlobalClusterRoutingDataSource.DataSourceType getCurrentDataSourceType()
    {
        return (GlobalClusterRoutingDataSource.DataSourceType) TransactionSynchronizationManager.getResource(GLOBAL_CLUSTER_DATA_SOURCE_TYPE_KEY);
    }

    public boolean isDataSourceCurrentlyReadOnly()
    {
        return TransactionSynchronizationManager.getResource(GLOBAL_CLUSTER_DATA_SOURCE_TYPE_KEY) == GlobalClusterRoutingDataSource.DataSourceType.SECONDARY;
    }

    public void setDataSourceType(GlobalClusterRoutingDataSource.DataSourceType dataSourceType)
    {
        TransactionSynchronizationManager.bindResource(GLOBAL_CLUSTER_DATA_SOURCE_TYPE_KEY, dataSourceType);
    }

    public void setDataSourceTypeFromReadOnlyFlag(boolean isReadonly)
    {
        GlobalClusterRoutingDataSource.DataSourceType dataSourceType = isReadonly ? GlobalClusterRoutingDataSource.DataSourceType.SECONDARY
                                                                                  : GlobalClusterRoutingDataSource.DataSourceType.PRIMARY;
        TransactionSynchronizationManager.bindResource(GLOBAL_CLUSTER_DATA_SOURCE_TYPE_KEY, dataSourceType);
    }

    public void cleanupDataSourceType()
    {
        TransactionSynchronizationManager.unbindResource(GLOBAL_CLUSTER_DATA_SOURCE_TYPE_KEY);
    }
}
