/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.attribute;

import java.util.UUID;

/**
 * 属性缓存驱逐事件
 *
 * @author baigod
 */
public record AttributeCacheEvictEvent(UUID entityId, String attrKey) {

    public AttributeCacheEvictEvent(UUID entityId) {
        this(entityId, null);
    }
}
