/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;

import java.io.Serializable;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class RedisCacheTransaction<K extends Serializable, V extends Serializable> implements CacheTransaction<K, V> {

    private final RedisTransactionalCache<K, V> cache;
    private final RedisConnection connection;

    @Override
    public void put(K key, V value) {
        cache.put(key, value, connection);
    }

    @Override
    public boolean commit() {
        try {
            var execResult = connection.exec();
            return execResult.stream().anyMatch(Objects::nonNull);
        } finally {
            connection.close();
        }
    }

    @Override
    public void rollback() {
        try {
            connection.discard();
        } finally {
            connection.close();
        }
    }

}
