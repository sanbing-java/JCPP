/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.cache;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.cache.Cache;

@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleCacheValueWrapper<T> implements CacheValueWrapper<T> {

    private final T value;

    @Override
    public T get() {
        return value;
    }

    public static <T> SimpleCacheValueWrapper<T> empty() {
        return new SimpleCacheValueWrapper<>(null);
    }

    public static <T> SimpleCacheValueWrapper<T> wrap(T value) {
        return new SimpleCacheValueWrapper<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> SimpleCacheValueWrapper<T> wrap(Cache.ValueWrapper source) {
        return source == null ? null : new SimpleCacheValueWrapper<>((T) source.get());
    }
}
