/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Data
@Component
public class QueueAppSettings {

    @Value("${queue.app.topic}")
    private String topic;

    @Value("${queue.app.partitions}")
    private int partitions;

    @Value("${queue.app.decoder:protobuf}")
    private DecoderType decoder;

    public enum DecoderType {
        protobuf,
        json
    }
}
