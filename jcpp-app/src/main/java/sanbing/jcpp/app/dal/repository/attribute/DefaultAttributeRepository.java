/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.repository.attribute;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import sanbing.jcpp.app.dal.entity.Attribute;
import sanbing.jcpp.app.dal.mapper.AttributeMapper;
import sanbing.jcpp.app.dal.repository.batch.ScheduledLogExecutorComponent;
import sanbing.jcpp.app.dal.repository.batch.SqlBlockingQueueParams;
import sanbing.jcpp.app.dal.repository.batch.SqlBlockingQueueWrapper;
import sanbing.jcpp.app.dal.repository.impl.RepositoryExecutorService;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.JCPPPair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DefaultAttributeRepository implements AttributeRepository {

    @Resource
    protected RepositoryExecutorService service;

    @Resource
    protected JdbcTemplate jdbcTemplate;

    @Resource
    protected TransactionTemplate transactionTemplate;

    @Resource
    ScheduledLogExecutorComponent logExecutor;

    @Resource
    private AttributeMapper attributeMapper;

    @Resource
    private AttributeKvInsertRepository attributeKvInsertRepository;

    @Resource
    private StatsFactory statsFactory;

    @Value("${sql.attributes.batch_size:1000}")
    private int batchSize;

    @Value("${sql.attributes.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.attributes.stats_print_interval_ms:1000}")
    private long statsPrintIntervalMs;

    @Value("${sql.attributes.batch_threads:4}")
    private int batchThreads;

    @Value("${sql.batch_sort:true}")
    private boolean batchSortEnabled;

    private SqlBlockingQueueWrapper<Attribute, Integer> queue;

    @PostConstruct
    private void init() {
        SqlBlockingQueueParams params = SqlBlockingQueueParams.builder()
                .logName("Attributes")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("attributes")
                .batchSortEnabled(batchSortEnabled)
                .withResponse(true)
                .build();

        Function<Attribute, Integer> hashcodeFunction = entity -> entity.getEntityId().hashCode();
        queue = new SqlBlockingQueueWrapper<>(params, hashcodeFunction, batchThreads, statsFactory);
        queue.init(logExecutor, v -> attributeKvInsertRepository.saveOrUpdate(v),
                Comparator.comparing(Attribute::getEntityId)
                        .thenComparing(Attribute::getAttrKey), l -> l
        );
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public Optional<AttributeKvEntry> find(UUID entityId, String attrKey) {
        Attribute attributeKvEntity = attributeMapper.findByEntityAndKey(entityId, attrKey);
        if (attributeKvEntity != null) {
            return Optional.ofNullable(attributeKvEntity.toData());
        }
        return Optional.empty();
    }

    @Override
    public List<AttributeKvEntry> find(UUID entityId, Collection<String> attrKeys) {
        List<Attribute> attributes = attributeMapper.findAllByIdAndAttrKey(entityId, attrKeys);
        return convertDataList(Lists.newArrayList(attributes));
    }

    @Override
    public List<AttributeKvEntry> findAll(UUID entityId) {
        List<Attribute> attributes = attributeMapper.findAllByEntityIdAndAttributeType(
                entityId);
        return convertDataList(Lists.newArrayList(attributes));
    }


    @Override
    public ListenableFuture<Integer> save(UUID entityId, AttributeKvEntry attribute) {
        Attribute entity = new Attribute();
        entity.setEntityId(entityId);
        entity.setAttrKey(attribute.getKey());
        entity.setLastUpdateTs(attribute.getLastUpdateTs());
        entity.setStrV(attribute.getStrValue().orElse(null));
        entity.setDblV(attribute.getDoubleValue().orElse(null));
        entity.setLongV(attribute.getLongValue().orElse(null));
        entity.setBoolV(attribute.getBooleanValue().orElse(null));
        entity.setJsonV(attribute.getJsonValue().orElse(null));
        return addToQueue(entity);
    }

    private ListenableFuture<Integer> addToQueue(Attribute entity) {
        return queue.add(entity);
    }

    @Override
    public List<ListenableFuture<String>> removeAll(UUID entityId, List<String> keys) {
        List<ListenableFuture<String>> futuresList = new ArrayList<>(keys.size());
        for (String key : keys) {
            futuresList.add(service.submit(() -> {
                attributeMapper.deleteByEntityIdAndKey(entityId, key);
                return key;
            }));
        }
        return futuresList;
    }

    @Override
    public List<ListenableFuture<JCPPPair<String, Integer>>> removeAllWithVersions(UUID entityId, List<String> keys) {
        List<ListenableFuture<JCPPPair<String, Integer>>> futuresList = new ArrayList<>(keys.size());
        for (String key : keys) {
            futuresList.add(service.submit(() -> {
                Integer version = transactionTemplate.execute(status -> jdbcTemplate.query("DELETE FROM t_attr WHERE entity_id = ? " +
                                "AND attr_key = ? RETURNING nextval('attr_kv_version_seq')",
                        rs -> rs.next() ? rs.getInt(1) : null, entityId, key));
                return JCPPPair.of(key, version);
            }));
        }
        return futuresList;
    }

    @Transactional
    @Override
    public List<String> removeAllByEntityId(UUID entityId) {
        return jdbcTemplate.queryForList("DELETE FROM t_attr WHERE entity_id = ? " +
                        "RETURNING attr_key", entityId).stream()
                .map(row -> row.get("attr_key").toString())
                .collect(Collectors.toList());
    }

    public static List<AttributeKvEntry> convertDataList(Collection<Attribute> toConvert) {
        if (CollectionUtils.isEmpty(toConvert)) {
            return Collections.emptyList();
        }
        List<AttributeKvEntry> converted = new ArrayList<>(toConvert.size());
        for (Attribute attribute : toConvert) {
            if (attribute != null) {
                converted.add(attribute.toData());
            }
        }
        return converted;
    }
}
