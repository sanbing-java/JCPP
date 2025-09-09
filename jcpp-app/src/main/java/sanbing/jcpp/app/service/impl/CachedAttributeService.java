/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.dal.repository.attribute.AttributeRepository;
import sanbing.jcpp.app.dal.repository.impl.RepositoryExecutorService;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.app.data.kv.AttributesSaveResult;
import sanbing.jcpp.app.data.kv.BaseAttributeKvEntry;
import sanbing.jcpp.app.service.AttributeService;
import sanbing.jcpp.app.service.cache.CacheExecutorService;
import sanbing.jcpp.app.service.cache.attribute.AttributeCacheKey;
import sanbing.jcpp.infrastructure.cache.CacheValueWrapper;
import sanbing.jcpp.infrastructure.cache.VersionedCache;
import sanbing.jcpp.infrastructure.stats.DefaultCounter;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.JCPPPair;
import sanbing.jcpp.infrastructure.util.validation.Validator;

import java.util.*;

import static sanbing.jcpp.app.dal.repository.attribute.KvValidator.validate;
import static sanbing.jcpp.app.dal.repository.attribute.KvValidator.validateId;


@Service
@Primary
@Slf4j
public class CachedAttributeService implements AttributeService {
    private static final String STATS_NAME = "attributes.cache";
    public static final String LOCAL_CACHE_TYPE = "caffeine";

    private final AttributeRepository attributeRepository;
    private final RepositoryExecutorService repositoryExecutorService;
    private final CacheExecutorService cacheExecutorService;
    private final DefaultCounter hitCounter;
    private final DefaultCounter missCounter;
    private final VersionedCache<AttributeCacheKey, AttributeKvEntry> cache;
    private ListeningExecutorService cacheExecutor;

    @Value("${cache.type:caffeine}")
    private String cacheType;
    @Value("${sql.attributes.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;

    public CachedAttributeService(AttributeRepository attributeRepository,
                                  RepositoryExecutorService repositoryExecutorService,
                                  StatsFactory statsFactory,
                                  CacheExecutorService cacheExecutorService,
                                  VersionedCache<AttributeCacheKey, AttributeKvEntry> cache) {
        this.attributeRepository = attributeRepository;
        this.repositoryExecutorService = repositoryExecutorService;
        this.cacheExecutorService = cacheExecutorService;
        this.cache = cache;

        this.hitCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "hit");
        this.missCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "miss");
    }

    @PostConstruct
    public void init() {
        this.cacheExecutor = getExecutor(cacheType, cacheExecutorService);
    }

    /**
     * Will return:
     * - for the <b>local</b> cache type (cache.type="coffeine"): directExecutor (run callback immediately in the same thread)
     * - for the <b>remote</b> cache: dedicated thread pool for the cache IO calls to unblock any caller thread
     */
    ListeningExecutorService getExecutor(String cacheType, CacheExecutorService cacheExecutorService) {
        if (StringUtils.isEmpty(cacheType) || LOCAL_CACHE_TYPE.equals(cacheType)) {
            log.info("Going to use directExecutor for the local cache type {}", cacheType);
            return MoreExecutors.newDirectExecutorService();
        }
        log.info("Going to use cacheExecutorService for the remote cache type {}", cacheType);
        return cacheExecutorService.executor();
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(UUID entityId, String attrKey) {
        validateId(entityId);
        Validator.validateString(attrKey, k -> "Incorrect attribute key " + k);

        return cacheExecutor.submit(() -> {
            AttributeCacheKey attributeCacheKey = new AttributeCacheKey( entityId, attrKey);
            CacheValueWrapper<AttributeKvEntry> cachedAttributeValue = cache.get(attributeCacheKey);
            if (cachedAttributeValue != null) {
                hitCounter.increment();
                AttributeKvEntry cachedAttributeKvEntry = cachedAttributeValue.get();
                return Optional.ofNullable(cachedAttributeKvEntry);
            } else {
                missCounter.increment();
                Optional<AttributeKvEntry> result = attributeRepository.find(entityId, attrKey);
                cache.put(attributeCacheKey, result.orElse(null));
                return result;
            }
        });
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(UUID entityId, final Collection<String> attrKeys) {
        validateId(entityId);
        final var attrKeySet = new LinkedHashSet<>(attrKeys); // deduplicate the attributes
        attrKeySet.forEach(attrKey -> Validator.validateString(attrKey, k -> "Incorrect attribute key " + k));

        //CacheExecutor for Redis or DirectExecutor for local Caffeine
        return Futures.transformAsync(cacheExecutor.submit(() -> findCachedAttributes(entityId,  attrKeySet)),
                wrappedCachedAttributes -> {

                    List<AttributeKvEntry> cachedAttributes = wrappedCachedAttributes.values().stream()
                            .map(CacheValueWrapper::get)
                            .filter(Objects::nonNull)
                            .toList();
                    if (wrappedCachedAttributes.size() == attrKeySet.size()) {
                        log.trace("[{}] Found all attributes from cache: {}", entityId,  attrKeySet);
                        return Futures.immediateFuture(cachedAttributes);
                    }

                    Set<String> notFoundAttrKeys = new HashSet<>(attrKeySet);
                    notFoundAttrKeys.removeAll(wrappedCachedAttributes.keySet());

                    // DB call should run in DB executor, not in cache-related executor
                    return repositoryExecutorService.submit(() -> {
                        log.trace("[{}] Lookup attributes from db: {}", entityId,  notFoundAttrKeys);
                        List<AttributeKvEntry> result = attributeRepository.find(entityId, notFoundAttrKeys);
                        for (AttributeKvEntry foundInDbAttribute : result) {
                            put(entityId, foundInDbAttribute);
                            notFoundAttrKeys.remove(foundInDbAttribute.getKey());
                        }
                        for (String key : notFoundAttrKeys) {
                            cache.put(new AttributeCacheKey(entityId, key), null);
                        }
                        List<AttributeKvEntry> mergedAttributes = new ArrayList<>(cachedAttributes);
                        mergedAttributes.addAll(result);
                        log.trace("[{}] Commit cache transaction: {}", entityId, notFoundAttrKeys);
                        return mergedAttributes;
                    });

                }, MoreExecutors.directExecutor()); // cacheExecutor analyse and returns results or submit to DB executor
    }

    private Map<String, CacheValueWrapper<AttributeKvEntry>> findCachedAttributes(UUID entityId, Collection<String> attrKeys) {
        Map<String, CacheValueWrapper<AttributeKvEntry>> cachedAttributes = new HashMap<>();
        for (String attrKey : attrKeys) {
            var cachedAttributeValue = cache.get(new AttributeCacheKey( entityId, attrKey));
            if (cachedAttributeValue != null) {
                hitCounter.increment();
                cachedAttributes.put(attrKey, cachedAttributeValue);
            } else {
                missCounter.increment();
            }
        }
        return cachedAttributes;
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(UUID entityId) {
        validateId(entityId);
        // We can`t watch on cache because the keys are unknown.
        return repositoryExecutorService.submit(() -> attributeRepository.findAll( entityId));
    }

    @Override
    public int removeAllByEntityId(UUID entityId) {
        List<String> result = attributeRepository.removeAllByEntityId(entityId);
        result.forEach(key -> {
                cache.evict(new AttributeCacheKey(entityId, key));
        });
        return result.size();
    }

    @Override
    public ListenableFuture<AttributesSaveResult> save(UUID entityId, AttributeKvEntry attribute) {
        validateId(entityId);
        validate(attribute, valueNoXssValidation);
        return doSave(entityId, List.of(attribute));
    }

    @Override
    public ListenableFuture<AttributesSaveResult> save(UUID entityId, List<AttributeKvEntry> attributes) {
        validateId(entityId);
        validate(attributes, valueNoXssValidation);
        return doSave( entityId,  attributes);
    }

    private ListenableFuture<AttributesSaveResult> doSave(UUID entityId, List<AttributeKvEntry> attributes) {
        List<ListenableFuture<Integer>> futures = new ArrayList<>(attributes.size());
        for (var attribute : attributes) {
            ListenableFuture<Integer> future = Futures.transform(attributeRepository.save(entityId, attribute), version -> {
                BaseAttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(((BaseAttributeKvEntry) attribute).getKv(), attribute.getLastUpdateTs(), version);
                put(entityId, attributeKvEntry);
                return version;
            }, cacheExecutor);
            futures.add(future);
        }
        return Futures.transform(Futures.allAsList(futures), AttributesSaveResult::of, MoreExecutors.directExecutor());
    }

    private void put(UUID entityId, AttributeKvEntry attribute) {
        String key = attribute.getKey();
        log.trace("[{}][{}] Before cache put: {}", entityId,  key, attribute);
        cache.put(new AttributeCacheKey( entityId, key), attribute);
        log.trace("[{}][{}] after cache put.", entityId,  key);
    }

    @Override
    public ListenableFuture<List<String>> removeAll(UUID entityId, List<String> attrKeys) {
        validateId(entityId);
        List<ListenableFuture<JCPPPair<String, Integer>>> futures = attributeRepository.removeAllWithVersions( entityId,  attrKeys);
        return Futures.allAsList(futures.stream().map(future -> Futures.transform(future, keyVersionPair -> {
            String key = keyVersionPair.getFirst();
            Integer version = keyVersionPair.getSecond();
            cache.evict(new AttributeCacheKey( entityId, key), version);
            return key;
        }, cacheExecutor)).toList());
    }

}
