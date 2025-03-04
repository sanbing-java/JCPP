/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.memory;


import sanbing.jcpp.infrastructure.queue.QueueMsg;

import java.util.List;

public interface InMemoryStorage {

    void printStats();

    int getLagTotal();

    int getLag(String topic);

    boolean put(String topic, QueueMsg msg);

    List<QueueMsg> get(String topic) throws InterruptedException;

}
