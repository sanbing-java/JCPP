/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import com.google.common.util.concurrent.ListenableFuture;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.app.data.kv.AttributesSaveResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttributeService {

    ListenableFuture<Optional<AttributeKvEntry>> find(UUID entityId, String attrKey);

    ListenableFuture<List<AttributeKvEntry>> find(UUID entityId, Collection<String> attrKeys);

    ListenableFuture<List<AttributeKvEntry>> findAll(UUID entityId);

    ListenableFuture<AttributesSaveResult> save(UUID entityId, List<AttributeKvEntry> attributes);

    ListenableFuture<AttributesSaveResult> save(UUID entityId, AttributeKvEntry attribute);

    ListenableFuture<List<String>> removeAll(UUID entityId, List<String> attrKeys);

    int removeAllByEntityId(UUID entityId);

}
