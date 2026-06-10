/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TransactionalCache<K extends Serializable, V extends Serializable> {

    String getCacheName();

    CacheValueWrapper<V> get(K key);

    void put(K key, V value);

    void putIfAbsent(K key, V value);

    void evict(K key);

    void evict(Collection<K> keys);

    void evictOrPut(K key, V value);

    CacheTransaction<K, V> newTransactionForKey(K key);

    CacheTransaction<K, V> newTransactionForKeys(List<K> keys);

    default V getOrFetchFromDB(K key, Supplier<V> dbCall, boolean cacheNullValue, boolean putToCache) {
        if (putToCache) {
            return getAndPutInTransaction(key, dbCall, cacheNullValue);
        } else {
            CacheValueWrapper<V> cacheValueWrapper = get(key);
            if (cacheValueWrapper != null) {
                return cacheValueWrapper.get();
            }
            return dbCall.get();
        }
    }

    default V getAndPutInTransaction(K key, Supplier<V> dbCall, boolean cacheNullValue) {
        return getAndPutInTransaction(key, dbCall, Function.identity(), Function.identity(), cacheNullValue);
    }

    default <R> R getAndPutInTransaction(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue) {
        CacheValueWrapper<V> cacheValueWrapper = get(key);
        if (cacheValueWrapper != null) {
            V cacheValue = cacheValueWrapper.get();
            return cacheValue != null ? cacheValueToResult.apply(cacheValue) : null;
        }
        var cacheTransaction = newTransactionForKey(key);
        try {
            R dbValue = dbCall.get();
            if (dbValue != null || cacheNullValue) {
                cacheTransaction.put(key, dbValueToCacheValue.apply(dbValue));
                cacheTransaction.commit();
                return dbValue;
            } else {
                cacheTransaction.rollback();
                return null;
            }
        } catch (Throwable e) {
            cacheTransaction.rollback();
            throw e;
        }
    }

    default <R> R getOrFetchFromDB(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue, boolean putToCache) {
        if (putToCache) {
            return getAndPutInTransaction(key, dbCall, cacheValueToResult, dbValueToCacheValue, cacheNullValue);
        } else {
            CacheValueWrapper<V> cacheValueWrapper = get(key);
            if (cacheValueWrapper != null) {
                var cacheValue = cacheValueWrapper.get();
                return cacheValue == null ? null : cacheValueToResult.apply(cacheValue);
            }
            return dbCall.get();
        }
    }

}
