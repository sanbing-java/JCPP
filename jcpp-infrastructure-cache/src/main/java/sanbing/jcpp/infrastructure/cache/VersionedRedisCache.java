/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.cache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;
import java.util.Arrays;

@Slf4j
public abstract class VersionedRedisCache<K extends VersionedCacheKey, V extends Serializable & HasVersion> extends RedisTransactionalCache<K, V> implements VersionedCache<K, V> {

    private static final int VERSION_SIZE = 8;
    private static final int VALUE_END_OFFSET = -1;

    static final byte[] SET_VERSIONED_VALUE_LUA_SCRIPT = StringRedisSerializer.UTF_8.serialize("""
            local key = KEYS[1]
            local newValue = ARGV[1]
            local newVersion = tonumber(ARGV[2])
            local expiration = tonumber(ARGV[3])

            local function setNewValue()
                local newValueWithVersion = struct.pack(">I8", newVersion) .. newValue
                redis.call('SET', key, newValueWithVersion, 'EX', expiration)
            end

            -- Get the current version (first 8 bytes) of the current value
            local currentVersionBytes = redis.call('GETRANGE', key, 0, 7)

            if currentVersionBytes and #currentVersionBytes == 8 then
                local currentVersion = struct.unpack(">I8", currentVersionBytes)
                if newVersion > currentVersion then
                    setNewValue()
                end
            else
                -- If the current value is absent or the current version is not found, set the new value
                setNewValue()
            end
            """);
    static final byte[] SET_VERSIONED_VALUE_SHA = StringRedisSerializer.UTF_8.serialize("0453cb1814135b706b4198b09a09f43c9f67bbfe");

    public VersionedRedisCache(String cacheName, CacheSpecsMap cacheSpecsMap, LettuceConnectionFactory connectionFactory, JCPPRedisCacheConfiguration configuration, JCPPRedisSerializer<K, V> valueSerializer) {
        super(cacheName, cacheSpecsMap, connectionFactory, configuration, valueSerializer);
    }

    @PostConstruct
    public void init() {
        try (var connection = getConnection(SET_VERSIONED_VALUE_SHA)) {
            log.debug("Loading LUA with expected SHA[{}], connection [{}]", new String(SET_VERSIONED_VALUE_SHA), connection.getNativeConnection());
            String sha = connection.scriptingCommands().scriptLoad(SET_VERSIONED_VALUE_LUA_SCRIPT);
            if (!Arrays.equals(SET_VERSIONED_VALUE_SHA, StringRedisSerializer.UTF_8.serialize(sha))) {
                log.error("SHA for SET_VERSIONED_VALUE_LUA_SCRIPT wrong! Expected [{}], but actual [{}], connection [{}]", new String(SET_VERSIONED_VALUE_SHA), sha, connection.getNativeConnection());
            }
        } catch (Throwable t) {
            log.error("Error on Redis versioned cache init", t);
        }
    }

    @Override
    protected byte[] doGet(K key, RedisConnection connection) {
        if (key.isVersioned()) {
            return super.doGet(key, connection);
        }
        byte[] rawKey = getRawKey(key);
        return connection.stringCommands().getRange(rawKey, VERSION_SIZE, VALUE_END_OFFSET);
    }

    @Override
    public void put(K key, V value) {
        if (key.isVersioned()) {
            super.put(key, value);
            return;
        }
        Integer version = getVersion(value);
        if (version == null) {
            return;
        }
        doPut(key, value, version, cacheTtl);
    }

    @Override
    public void put(K key, V value, RedisConnection connection) {
        if (key.isVersioned()) {
            super.put(key, value, connection); // because scripting commands are not supported in transaction mode
            return;
        }
        Integer version = getVersion(value);
        if (version == null) {
            return;
        }
        byte[] rawKey = getRawKey(key);
        doPut(rawKey, value, version, cacheTtl, connection);
    }

    private void doPut(K key, V value, Integer version, Expiration expiration) {
        if (!cacheEnabled) {
            return;
        }
        log.trace("put [{}][{}][{}]", key, value, version);
        final byte[] rawKey = getRawKey(key);
        try (var connection = getConnection(rawKey)) {
            doPut(rawKey, value, version, expiration, connection);
        }
    }

    private void doPut(byte[] rawKey, V value, Integer version, Expiration expiration, RedisConnection connection) {
        byte[] rawValue = getRawValue(value);
        byte[] rawVersion = StringRedisSerializer.UTF_8.serialize(String.valueOf(version));
        byte[] rawExpiration = StringRedisSerializer.UTF_8.serialize(String.valueOf(expiration.getExpirationTimeInSeconds()));
        try {
            connection.scriptingCommands().evalSha(SET_VERSIONED_VALUE_SHA, ReturnType.VALUE, 1, rawKey, rawValue, rawVersion, rawExpiration);
        } catch (InvalidDataAccessApiUsageException e) {
            log.debug("loading LUA [{}]", connection.getNativeConnection());
            String sha = connection.scriptingCommands().scriptLoad(SET_VERSIONED_VALUE_LUA_SCRIPT);
            if (!Arrays.equals(SET_VERSIONED_VALUE_SHA, StringRedisSerializer.UTF_8.serialize(sha))) {
                log.error("SHA for SET_VERSIONED_VALUE_LUA_SCRIPT wrong! Expected [{}], but actual [{}]", new String(SET_VERSIONED_VALUE_SHA), sha);
            }
            try {
                connection.scriptingCommands().evalSha(SET_VERSIONED_VALUE_SHA, ReturnType.VALUE, 1, rawKey, rawValue, rawVersion, rawExpiration);
            } catch (InvalidDataAccessApiUsageException ignored) {
                log.debug("Slowly executing eval instead of fast evalsha");
                connection.scriptingCommands().eval(SET_VERSIONED_VALUE_LUA_SCRIPT, ReturnType.VALUE, 1, rawKey, rawValue, rawVersion, rawExpiration);
            }
        }
    }

    @Override
    public void evict(K key, Integer version) {
        log.trace("evict [{}][{}]", key, version);
        if (version != null) {
            doPut(key, null, version, evictExpiration);
        }
    }

    @Override
    public void putIfAbsent(K key, V value) {
        throw new NotImplementedException("putIfAbsent is not supported by versioned cache");
    }

    @Override
    public void evictOrPut(K key, V value) {
        throw new NotImplementedException("evictOrPut is not supported by versioned cache");
    }

}
