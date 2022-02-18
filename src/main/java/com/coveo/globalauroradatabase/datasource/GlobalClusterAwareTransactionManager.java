/*
 * Copyright (c) Coveo Solutions Inc.
 */
package com.coveo.globalauroradatabase.datasource;

import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class GlobalClusterAwareTransactionManager implements PlatformTransactionManager
{
    private static final String GLOBAL_CLUSTER_AWARE_TRANSACTION_SYNCHRONIZATION_KEY = GlobalClusterAwareTransactionManager.class.getName()
            + ".transactionSynchronization";

    private final PlatformTransactionManager wrappedPlatformTransactionManager;

    public GlobalClusterAwareTransactionManager(PlatformTransactionManager wrappedPlatformTransactionManager)
    {
        this.wrappedPlatformTransactionManager = Objects.requireNonNull(wrappedPlatformTransactionManager);
    }

    @Override
    public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
            throws TransactionException
    {
        definition = Optional.ofNullable(definition).orElseGet(TransactionDefinition::withDefaults);

        GlobalClusterAwareTransactionSynchronization globalClusterAwareTransactionSynchronization = getOrInitializeGlobalClusterAwareTransactionSynchronization();
        // This is needed in case we encounter nested transactions. It will be used to properly suspend or resume transactions and set the proper data source at the right time.
        globalClusterAwareTransactionSynchronization.setCurrentTransactionDataSourceType(definition.isReadOnly());

        boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean requiresNew = TransactionDefinition.PROPAGATION_REQUIRES_NEW == definition.getPropagationBehavior();
        boolean isCurrentlyReadOnly = globalClusterAwareTransactionSynchronization.isDataSourceCurrentlyReadOnly();
        if (isTxActive && !requiresNew && isCurrentlyReadOnly && !definition.isReadOnly()) {
            throw new CannotCreateTransactionException("Cannot request read/write transaction from initialized read only transaction.");
        }
        // Set data source when starting a new transaction, otherwise let suspend/resume handle nested transaction data source changes if needed.
        if (!isTxActive) {
            globalClusterAwareTransactionSynchronization.setDataSourceTypeFromReadOnlyFlag(definition.isReadOnly());
        }
        try {
            TransactionStatus transactionStatus = wrappedPlatformTransactionManager.getTransaction(definition);
            // registerSynchronization needs to be done after getTransaction since synchronization is initialized in getTransaction.
            // This register is needed to be notified when transactions are suspended, resumed or completed.
            TransactionSynchronizationManager.registerSynchronization(globalClusterAwareTransactionSynchronization);
            return transactionStatus;
        } catch (Throwable e) {
            globalClusterAwareTransactionSynchronization.cleanupDataSourceType();
            throw e;
        }
    }

    @Override
    public final void commit(TransactionStatus status) throws TransactionException
    {
        wrappedPlatformTransactionManager.commit(status);
    }

    @Override
    public final void rollback(TransactionStatus status) throws TransactionException
    {
        wrappedPlatformTransactionManager.rollback(status);
    }

    private GlobalClusterAwareTransactionSynchronization getOrInitializeGlobalClusterAwareTransactionSynchronization()
    {
        GlobalClusterAwareTransactionSynchronization globalClusterAwareTransactionSynchronization;

        Object resource = TransactionSynchronizationManager.getResource(GLOBAL_CLUSTER_AWARE_TRANSACTION_SYNCHRONIZATION_KEY);
        if (resource != null) {
            globalClusterAwareTransactionSynchronization = (GlobalClusterAwareTransactionSynchronization) resource;
        } else {
            globalClusterAwareTransactionSynchronization = new GlobalClusterAwareTransactionSynchronization();
            TransactionSynchronizationManager.bindResource(GLOBAL_CLUSTER_AWARE_TRANSACTION_SYNCHRONIZATION_KEY,
                                                           globalClusterAwareTransactionSynchronization);
        }
        return globalClusterAwareTransactionSynchronization;
    }
}
