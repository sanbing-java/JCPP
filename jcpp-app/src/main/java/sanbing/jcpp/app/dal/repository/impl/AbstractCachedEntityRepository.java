/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.repository.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import sanbing.jcpp.infrastructure.cache.TransactionalCache;

import java.io.Serializable;

public abstract class AbstractCachedEntityRepository<K extends Serializable, V extends Serializable, E> extends AbstractEntityRepository {

    @Autowired
    protected TransactionalCache<K, V> cache;

    protected void publishEvictEvent(E event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }

    public abstract void handleEvictEvent(E event);

}
