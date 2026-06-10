/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue;

import lombok.Data;

@Data
public class DefaultQueueMsg implements QueueMsg {
    private final String key;
    private final byte[] data;
    private final DefaultQueueMsgHeaders headers;

    public DefaultQueueMsg(QueueMsg msg) {
        this.key = msg.getKey();
        this.data = msg.getData();
        DefaultQueueMsgHeaders headers = new DefaultQueueMsgHeaders();
        msg.getHeaders().getData().forEach(headers::put);
        this.headers = headers;
    }

}
