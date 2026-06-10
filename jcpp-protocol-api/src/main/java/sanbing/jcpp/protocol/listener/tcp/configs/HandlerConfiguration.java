/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.listener.tcp.configs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import sanbing.jcpp.protocol.cfg.enums.TcpHandlerType;

@JsonTypeInfo(
        use = Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @Type(
                value = TextHandlerConfiguration.class,
                name = "TEXT"
        ),
        @Type(
                value = BinaryHandlerConfiguration.class,
                name = "BINARY"
        ),
        @Type(
                value = JsonHandlerConfiguration.class,
                name = "JSON"
        )
})
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public interface HandlerConfiguration {
    TcpHandlerType getType();
}
