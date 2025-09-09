/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sanbing.jcpp.infrastructure.util.async.AbstractListeningExecutor;

@Component
public class CacheExecutorService extends AbstractListeningExecutor {

    @Value("${cache.maximumPoolSize}")
    private int poolSize;

    @Override
    protected int getThreadPollSize() {
        return poolSize;
    }

}
