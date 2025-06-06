/**
 * Copyright (c) 2013-2024 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.command;

import org.redisson.api.BatchOptions;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.BatchCommandData;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.command.CommandBatchService.Entry;
import org.redisson.config.DelayStrategy;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.NodeSource;
import org.redisson.liveobject.core.RedissonObjectBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author Nikita Koksharov
 *
 * @param <V> type of value
 * @param <R> type of returned value
 */
public class BaseRedisBatchExecutor<V, R> extends RedisExecutor<V, R> {

    final ConcurrentMap<NodeSource, Entry> commands;
    final BatchOptions options;
    final AtomicInteger index;
    
    final AtomicBoolean executed;
    
    @SuppressWarnings("ParameterNumber")
    public BaseRedisBatchExecutor(boolean readOnlyMode, NodeSource source, Codec codec, RedisCommand<V> command,
                                  Object[] params, CompletableFuture<R> mainPromise, boolean ignoreRedirect,
                                  ConnectionManager connectionManager, RedissonObjectBuilder objectBuilder,
                                  ConcurrentMap<NodeSource, Entry> commands,
                                  BatchOptions options, AtomicInteger index, AtomicBoolean executed, RedissonObjectBuilder.ReferenceType referenceType,
                                  boolean noRetry) {
        
        super(readOnlyMode, source, codec, command, params, mainPromise, ignoreRedirect, connectionManager,
                objectBuilder, referenceType, noRetry,
                retryAttempts(connectionManager, options),
                retryInterval(connectionManager, options),
                timeout(connectionManager, options),
                false);
        this.commands = commands;
        this.options = options;
        this.index = index;
        this.executed = executed;
    }

    private static int timeout(ConnectionManager connectionManager, BatchOptions options) {
        int result = connectionManager.getServiceManager().getConfig().getTimeout();
        if (options.getResponseTimeout() > 0) {
            result = (int) options.getResponseTimeout();
        }
        if (options.getSyncSlaves() > 0) {
            result += (int) options.getSyncTimeout();
        }
        return result;
    }

    private static DelayStrategy retryInterval(ConnectionManager connectionManager, BatchOptions options) {
        if (options.getRetryDelay() != null) {
            return options.getRetryDelay();
        }
        return connectionManager.getServiceManager().getConfig().getRetryDelay();
    }

    private static int retryAttempts(ConnectionManager connectionManager, BatchOptions options) {
        if (options.getRetryAttempts() >= 0) {
            return options.getRetryAttempts();
        }
        return connectionManager.getServiceManager().getConfig().getRetryAttempts();
    }

    protected final void addBatchCommandData(Object[] batchParams) {
        Entry entry = commands.computeIfAbsent(source, k -> new Entry());

        if (!readOnlyMode) {
            entry.setReadOnlyMode(false);
        }

        Codec codecToUse = getCodec(codec);
        BatchCommandData<V, R> commandData = new BatchCommandData<>(mainPromise, codecToUse, command, batchParams, index.incrementAndGet());
        entry.addCommand(commandData);
    }
        
}
