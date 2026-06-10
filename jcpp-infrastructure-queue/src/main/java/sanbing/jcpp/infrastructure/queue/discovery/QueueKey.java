/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.discovery;

import lombok.Data;
import lombok.With;

@Data
public class QueueKey {
    public static final String MAIN_QUEUE_NAME = "Main";

    private final ServiceType type;
    @With
    private final String queueName;

    public QueueKey(ServiceType type, String queueName) {
        this.type = type;
        this.queueName = queueName;
    }

    public QueueKey(ServiceType type) {
        this.type = type;
        this.queueName = MAIN_QUEUE_NAME;
    }

    @Override
    public String toString() {
        return "QK(" + queueName + "," + type + ")";
    }

}
