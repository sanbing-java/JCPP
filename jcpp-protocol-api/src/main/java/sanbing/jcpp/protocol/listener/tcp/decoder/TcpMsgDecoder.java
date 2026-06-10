/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.listener.tcp.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.protocol.domain.ProtocolUplinkMsg;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public class TcpMsgDecoder<T> extends MessageToMessageDecoder<ByteBuf> {
    private final String protocolName;
    private final Function<ByteBuf, T> transformer;

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        try {
            out.add(new ProtocolUplinkMsg<>(ctx.pipeline().channel().remoteAddress(), UUID.randomUUID(), this.transformer.apply(msg), msg.readableBytes()));
        } catch (Exception e) {
            log.error("[{}][{}] Exception during of decoding message", protocolName, ctx.channel(), e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] toByteArray(ByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    public static String toString(ByteBuf buffer, String charsetName) {
        return buffer.toString(Charset.forName(charsetName));
    }

    public static JsonNode toJson(ByteBuf buffer) {
        return JacksonUtil.fromBytes(toByteArray(buffer));
    }

}