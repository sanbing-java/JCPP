/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.discovery.event;

import lombok.Getter;

public class OtherServiceShutdownEvent extends JCPPApplicationEvent {

    @Getter
    private final String serviceId;

    public OtherServiceShutdownEvent(Object source, String serviceId) {
        super(source);
        this.serviceId = serviceId;
    }
}
