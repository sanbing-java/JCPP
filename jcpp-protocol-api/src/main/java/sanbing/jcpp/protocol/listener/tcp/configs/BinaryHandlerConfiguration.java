/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.listener.tcp.configs;

import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import sanbing.jcpp.protocol.cfg.enums.TcpHandlerType;

import static sanbing.jcpp.protocol.cfg.enums.TcpHandlerType.BINARY;

@Data
@ToString
@EqualsAndHashCode
public class BinaryHandlerConfiguration implements HandlerConfiguration {
    public static final String LITTLE_ENDIAN_BYTE_ORDER = "LITTLE_ENDIAN";

    /**
     * 拆包器
     */
    private Class<? extends ByteToMessageDecoder> decoder;

    /**
     * 大小端（共用）
     */
    private String byteOrder;

    /**
     * 起始域HEX字符串
     */
    private String head;

    /**
     * 结束域（HeadTailFrameDecoder)
     */
    private String tail;

    /**
     * 最大帧长（LengthFieldBasedFrameDecoder）
     */
    private int maxFrameLength;

    /**
     * 长度域位置（共用）
     */
    private int lengthFieldOffset;

    /**
     * 长度域长度（共用）
     */
    private int lengthFieldLength;

    /**
     * 长度调整（共用）
     */
    private int lengthAdjustment;

    /**
     * 初始跳过字节数（共用）
     */
    private int initialBytesToStrip;

    /**
     * 快速失败（LengthFieldBasedFrameDecoder）
     */
    private boolean failFast;

    public TcpHandlerType getType() {
        return BINARY;
    }

}
