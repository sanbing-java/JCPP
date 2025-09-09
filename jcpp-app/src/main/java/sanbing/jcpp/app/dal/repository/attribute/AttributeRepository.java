/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.repository.attribute;

import com.google.common.util.concurrent.ListenableFuture;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.infrastructure.util.JCPPPair;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttributeRepository {

    Optional<AttributeKvEntry> find(UUID entityId, String attrKey);

    List<AttributeKvEntry> find(UUID entityId, Collection<String> attrKeys);

    List<AttributeKvEntry> findAll( UUID entityId);

    ListenableFuture<Integer> save(UUID entityId, AttributeKvEntry attribute);

    List<ListenableFuture<String>> removeAll(UUID entityId, List<String> keys);

    List<ListenableFuture<JCPPPair<String, Integer>>> removeAllWithVersions(UUID entityId, List<String> keys);

    List<String> removeAllByEntityId(UUID entityId);

}
