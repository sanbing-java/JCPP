/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.cache;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.core.*;
import sanbing.jcpp.AbstractTestBase;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

@EnabledIfSystemProperty(named = "cache.type", matches = "redis")
class RedisCacheConfigurationIT extends AbstractTestBase {

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Resource
    ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    final static int testTimes = 10_000;
    final static String hashKey = "hashKey";

    @Test
    @Order(1)
    void kvTest() {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        IntStream.range(0, testTimes).forEach(i -> {
            String key = "field:" + i;
            String value = "value:" + i;
            valueOperations.set(key, value, Duration.ofMinutes(1));
        });

        Object o = valueOperations.get("field:1000");
        System.out.println(Objects.requireNonNull(o).getClass() + " : " + o);
    }

    @Test
    @Order(2)
    void hashTest() {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        IntStream.range(0, testTimes).forEach(i -> {
            String key = "field:" + i;
            String value = "value:" + i;
            hashOperations.put(hashKey, key, value);
        });

        redisTemplate.expire(hashKey, Duration.ofMinutes(1));

        Map<Object, Object> slowKey = hashOperations.entries(hashKey);
        System.out.println("map size:" + slowKey.size());
    }

    @Test
    @Order(3)
    void reactiveKVTest() {
        ReactiveValueOperations<String, Object> valueOperations = reactiveRedisTemplate.opsForValue();

        IntStream.range(0, testTimes).forEach(i -> {
            String key = "field:" + i;
            String value = "value:" + i;
            valueOperations.set(key, value, Duration.ofMinutes(1)).block();
        });

        Object o = valueOperations.get("field:1000").block();
        System.out.println(Objects.requireNonNull(o).getClass() + " : " + o);
    }

    @Test
    @Order(4)
    void reactiveHashTest() throws InterruptedException {
        ReactiveHashOperations<String, Object, Object> hashOperations = reactiveRedisTemplate.opsForHash();

        IntStream.range(0, testTimes).forEach(i -> {
            String key = "field:" + i;
            String value = "value:" + i;
            hashOperations.put(hashKey, key, value).block();
        });

        redisTemplate.expire(hashKey, Duration.ofMinutes(1));

        CountDownLatch latch = new CountDownLatch(1);
        hashOperations.entries(hashKey).collectList().subscribe(entries -> {
            System.out.println("size:" + entries.size());
            latch.countDown();
        });
        latch.await();
    }
}