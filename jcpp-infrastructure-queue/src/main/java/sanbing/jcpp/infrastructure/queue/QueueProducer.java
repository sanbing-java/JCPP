/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue;


import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;

public interface QueueProducer<T extends QueueMsg> {

    void init();

    String getTopic();

    void send(TopicPartitionInfo tpi, T msg, QueueCallback callback);

    void stop();
}
