/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue;

import com.google.protobuf.GeneratedMessage;
import lombok.Data;

@Data
public class ProtoQueueMsg<T extends GeneratedMessage> implements QueueMsg {

    private final String key;
    protected final T value;
    private final QueueMsgHeaders headers;

    public ProtoQueueMsg(String key, T value) {
        this(key, value, new DefaultQueueMsgHeaders());
    }

    public ProtoQueueMsg(String key, T value, QueueMsgHeaders headers) {
        this.key = key;
        this.value = value;
        this.headers = headers;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public QueueMsgHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return value.toByteArray();
    }
}
