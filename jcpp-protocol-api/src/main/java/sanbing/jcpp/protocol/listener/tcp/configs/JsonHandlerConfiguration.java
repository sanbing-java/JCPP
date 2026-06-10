/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.listener.tcp.configs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import sanbing.jcpp.protocol.cfg.enums.TcpHandlerType;

import static sanbing.jcpp.protocol.cfg.enums.TcpHandlerType.JSON;

@Data
@ToString
@EqualsAndHashCode
public class JsonHandlerConfiguration implements HandlerConfiguration {
    public TcpHandlerType getType() {
        return JSON;
    }

}
