/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.memory;

import lombok.Data;
import sanbing.jcpp.infrastructure.queue.QueueCallback;
import sanbing.jcpp.infrastructure.queue.QueueMsg;
import sanbing.jcpp.infrastructure.queue.QueueProducer;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;

@Data
public class InMemoryQueueProducer<T extends QueueMsg> implements QueueProducer<T> {
    private final InMemoryStorage storage;

    private final String topic;

    public InMemoryQueueProducer(InMemoryStorage storage, String topic) {
        this.storage = storage;
        this.topic = topic;
    }

    @Override
    public void init() {

    }

    @Override
    public void send(TopicPartitionInfo tpi, T msg, QueueCallback callback) {
        boolean result = storage.put(tpi.getFullTopicName(), msg);
        if (result) {
            if (callback != null) {
                callback.onSuccess(null);
            }
        } else {
            if (callback != null) {
                callback.onFailure(new RuntimeException("Failure add msg to InMemoryQueue"));
            }
        }
    }

    @Override
    public void stop() {

    }

}
