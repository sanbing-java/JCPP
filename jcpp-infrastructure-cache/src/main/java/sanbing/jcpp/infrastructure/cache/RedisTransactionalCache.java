/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.cache;

import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class RedisTransactionalCache<K extends Serializable, V extends Serializable> implements TransactionalCache<K, V> {

    static final byte[] BINARY_NULL_VALUE = RedisSerializer.java().serialize(NullValue.INSTANCE);

    @Getter
    private final String cacheName;
    @Getter
    private final LettuceConnectionFactory connectionFactory;
    private final RedisSerializer<String> keySerializer = StringRedisSerializer.UTF_8;
    private final JCPPRedisSerializer<K, V> valueSerializer;
    protected final Expiration evictExpiration;
    protected final Expiration cacheTtl;
    protected final boolean cacheEnabled;

    public RedisTransactionalCache(String cacheName,
                                   CacheSpecsMap cacheSpecsMap,
                                   LettuceConnectionFactory connectionFactory,
                                   JCPPRedisCacheConfiguration configuration,
                                   JCPPRedisSerializer<K, V> valueSerializer) {
        this.cacheName = cacheName;
        this.connectionFactory = connectionFactory;
        this.valueSerializer = valueSerializer;
        this.evictExpiration = Expiration.from(configuration.getEvictTtlInMs(), TimeUnit.MILLISECONDS);
        this.cacheTtl = Optional.ofNullable(cacheSpecsMap)
                .map(CacheSpecsMap::getSpecs)
                .map(specs -> specs.get(cacheName))
                .map(CacheSpecs::getTimeToLiveInMinutes)
                .filter(ttl -> !ttl.equals(0))
                .map(ttl -> Expiration.from(ttl, TimeUnit.MINUTES))
                .orElseGet(Expiration::persistent);
        this.cacheEnabled = Optional.ofNullable(cacheSpecsMap)
                .map(CacheSpecsMap::getSpecs)
                .map(x -> x.get(cacheName))
                .map(CacheSpecs::getMaxSize)
                .map(size -> size > 0)
                .orElse(false);
    }

    @Override
    public CacheValueWrapper<V> get(K key) {
        if (!cacheEnabled) {
            return null;
        }
        try (var connection = connectionFactory.getConnection()) {
            byte[] rawValue = doGet(key, connection);
            if (rawValue == null || rawValue.length == 0) {
                return null;
            } else if (Arrays.equals(rawValue, BINARY_NULL_VALUE)) {
                return SimpleCacheValueWrapper.empty();
            } else {
                V value = valueSerializer.deserialize(key, rawValue);
                return SimpleCacheValueWrapper.wrap(value);
            }
        }
    }

    protected byte[] doGet(K key, RedisConnection connection) {
        return connection.stringCommands().get(getRawKey(key));
    }

    @Override
    public void put(K key, V value) {
        if (!cacheEnabled) {
            return;
        }
        try (var connection = connectionFactory.getConnection()) {
            put(key, value, connection);
        }
    }

    public void put(K key, V value, RedisConnection connection) {
        put(connection, key, value, RedisStringCommands.SetOption.UPSERT);
    }

    @Override
    public void putIfAbsent(K key, V value) {
        if (!cacheEnabled) {
            return;
        }
        try (var connection = connectionFactory.getConnection()) {
            put(connection, key, value, RedisStringCommands.SetOption.SET_IF_ABSENT);
        }
    }

    @Override
    public void evict(K key) {
        if (!cacheEnabled) {
            return;
        }
        try (var connection = connectionFactory.getConnection()) {
            connection.keyCommands().del(getRawKey(key));
        }
    }

    @Override
    public void evict(Collection<K> keys) {
        if (!cacheEnabled) {
            return;
        }
        if (keys.isEmpty()) {
            return;
        }
        try (var connection = connectionFactory.getConnection()) {
            connection.keyCommands().del(keys.stream().map(this::getRawKey).toArray(byte[][]::new));
        }
    }

    @Override
    public void evictOrPut(K key, V value) {
        if (!cacheEnabled) {
            return;
        }
        try (var connection = connectionFactory.getConnection()) {
            var rawKey = getRawKey(key);
            var records = connection.keyCommands().del(rawKey);
            if (records == null || records == 0) {
                //We need to put the value in case of Redis, because evict will NOT cancel concurrent transaction used to "get" the missing value from cache.
                connection.stringCommands().set(rawKey, getRawValue(value), evictExpiration, RedisStringCommands.SetOption.UPSERT);
            }
        }
    }

    @Override
    public CacheTransaction<K, V> newTransactionForKey(K key) {
        byte[][] rawKey = new byte[][]{getRawKey(key)};
        RedisConnection connection = watch(rawKey);
        return new RedisCacheTransaction<>(this, connection);
    }

    @Override
    public CacheTransaction<K, V> newTransactionForKeys(List<K> keys) {
        RedisConnection connection = watch(keys.stream().map(this::getRawKey).toArray(byte[][]::new));
        return new RedisCacheTransaction<>(this, connection);
    }

    @Override
    public <R> R getAndPutInTransaction(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue) {
        if (!cacheEnabled) {
            return dbCall.get();
        }
        return TransactionalCache.super.getAndPutInTransaction(key, dbCall, cacheValueToResult, dbValueToCacheValue, cacheNullValue);
    }

    @SuppressWarnings("unchecked")
    protected RedisConnection getConnection(byte[] rawKey) {
        if (!connectionFactory.isClusterAware()) {
            return connectionFactory.getConnection();
        }

        RedisClusterNode redisClusterNode = connectionFactory.getClusterConnection().clusterGetNodeForKey(rawKey);
        Object nativeConnection = connectionFactory.getConnection().getNativeConnection();
        RedisClusterAsyncCommands<?,?> connection = ((RedisAdvancedClusterAsyncCommandsImpl<?,?>) nativeConnection).getConnection(redisClusterNode.getId());
        LettuceConnection lettuceConnection = new LettuceConnection(((RedisAsyncCommandsImpl) connection).getStatefulConnection(),
                connectionFactory.getTimeout(),
                RedisClient.create());
        lettuceConnection.setConvertPipelineAndTxResults(connectionFactory.getConvertPipelineAndTxResults());
        return lettuceConnection;
    }

    protected RedisConnection watch(byte[][] rawKeysList) {
        RedisConnection connection = getConnection(rawKeysList[0]);
        try {
            connection.watch(rawKeysList);
            connection.multi();
        } catch (Exception e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    protected byte[] getRawKey(K key) {
        String keyString = cacheName + key.toString();
        byte[] rawKey;
        try {
            rawKey = keySerializer.serialize(keyString);
        } catch (Exception e) {
            log.warn("Failed to serialize the cache key: {}", key, e);
            throw new RuntimeException(e);
        }
        if (rawKey == null) {
            log.warn("Failed to serialize the cache key: {}", key);
            throw new IllegalArgumentException("Failed to serialize the cache key!");
        }
        return rawKey;
    }

    protected byte[] getRawValue(V value) {
        if (value == null) {
            return BINARY_NULL_VALUE;
        } else {
            try {
                return valueSerializer.serialize(value);
            } catch (Exception e) {
                log.warn("Failed to serialize the cache value: {}", value, e);
                throw new RuntimeException(e);
            }
        }
    }

    public void put(RedisConnection connection, K key, V value, RedisStringCommands.SetOption setOption) {
        if (!cacheEnabled) {
            return;
        }
        byte[] rawKey = getRawKey(key);
        put(connection, rawKey, value, setOption);
    }

    public void put(RedisConnection connection, byte[] rawKey, V value, RedisStringCommands.SetOption setOption) {
        byte[] rawValue = getRawValue(value);
        connection.stringCommands().set(rawKey, rawValue, this.cacheTtl, setOption);
    }

}
