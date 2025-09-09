/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.listener;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.protocol.cfg.TcpCfg;
import sanbing.jcpp.protocol.cfg.TcpHandlerCfg;
import sanbing.jcpp.protocol.cfg.enums.TcpHandlerType;
import sanbing.jcpp.protocol.listener.tcp.TcpChannelHandler;
import sanbing.jcpp.protocol.listener.tcp.configs.BinaryHandlerConfiguration;
import sanbing.jcpp.protocol.listener.tcp.configs.TextHandlerConfiguration;
import sanbing.jcpp.protocol.listener.tcp.decoder.JCPPHeadTailFrameDecoder;
import sanbing.jcpp.protocol.listener.tcp.decoder.JCPPLengthFieldBasedFrameDecoder;
import sanbing.jcpp.protocol.listener.tcp.decoder.TcpMsgDecoder;
import sanbing.jcpp.protocol.listener.tcp.handler.ConnectionLimitHandler;
import sanbing.jcpp.protocol.listener.tcp.handler.IdleEventHandler;
import sanbing.jcpp.protocol.listener.tcp.handler.TracerHandler;

import java.nio.ByteOrder;

import static sanbing.jcpp.protocol.cfg.enums.TcpHandlerType.BINARY;
import static sanbing.jcpp.protocol.cfg.enums.TcpHandlerType.TEXT;
import static sanbing.jcpp.protocol.listener.tcp.configs.BinaryHandlerConfiguration.LITTLE_ENDIAN_BYTE_ORDER;
import static sanbing.jcpp.protocol.listener.tcp.configs.TextHandlerConfiguration.SYSTEM_LINE_SEPARATOR;

/**
 * @author 九筒
 */
@Slf4j
@RequiredArgsConstructor
public abstract class ChannelHandlerInitializer<C extends Channel> extends ChannelInitializer<C> {

    protected final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected abstract void initChannel(C ch);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    public static ChannelHandlerInitializer<SocketChannel> createTcpChannelHandler(TcpCfg tcpCfg, ChannelHandlerParameter parameter) {
        TcpHandlerCfg tcpCfgHandler = tcpCfg.getHandler();
        TcpHandlerType type = tcpCfgHandler.getType();

        return switch (type) {
            case TEXT -> new ChannelHandlerInitializer<>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    TextHandlerConfiguration textHandlerConfig = (TextHandlerConfiguration) tcpCfgHandler.getConfiguration(TEXT);
                    ByteBuf[] delimiters = SYSTEM_LINE_SEPARATOR.equals(textHandlerConfig.getMessageSeparator())
                            ? Delimiters.lineDelimiter() : Delimiters.nulDelimiter();
                    DelimiterBasedFrameDecoder framer = new DelimiterBasedFrameDecoder(textHandlerConfig.getMaxFrameLength(),
                            textHandlerConfig.isStripDelimiter(), delimiters);
                    socketChannel.pipeline()
                            .addLast("tracerHandler", new TracerHandler())
                            .addLast("connectionLimitHandler", new ConnectionLimitHandler(parameter.protocolName(), tcpCfgHandler.getMaxConnections(), CHANNEL_GROUP, parameter.connectionsGauge()))
                            .addLast("idleStateHandler", new IdleStateHandler(tcpCfgHandler.getIdleTimeoutSeconds(), 0, 0))
                            .addLast("idleEventHandler", new IdleEventHandler(parameter.protocolName()))
                            .addLast("framer", framer)
                            .addLast("tcpTextDecoder", new TcpMsgDecoder<>(parameter.protocolName(), msg -> TcpMsgDecoder.toString(msg, textHandlerConfig.getCharsetName())))
                            .addLast("tcpStringInHandler", new TcpChannelHandler<>(parameter));

                }
            };
            case JSON -> new ChannelHandlerInitializer<>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline()
                            .addLast("tracerHandler", new TracerHandler())
                            .addLast("connectionLimitHandler", new ConnectionLimitHandler(parameter.protocolName(), tcpCfgHandler.getMaxConnections(), CHANNEL_GROUP, parameter.connectionsGauge()))
                            .addLast("idleStateHandler",
                                    new IdleStateHandler(tcpCfgHandler.getIdleTimeoutSeconds(), 0, 0))
                            .addLast("idleEventHandler", new IdleEventHandler(parameter.protocolName()))
                            .addLast("datagramToJsonDecoder", new JsonObjectDecoder())
                            .addLast("tcpJsonDecoder", new TcpMsgDecoder<>(parameter.protocolName(), TcpMsgDecoder::toJson))
                            .addLast("tcpJsonInHandler", new TcpChannelHandler<>(parameter));
                }
            };
            case BINARY -> new ChannelHandlerInitializer<>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    BinaryHandlerConfiguration binaryHandlerConfig = (BinaryHandlerConfiguration) tcpCfgHandler.getConfiguration(BINARY);

                    ByteOrder byteOrder = LITTLE_ENDIAN_BYTE_ORDER.equalsIgnoreCase(binaryHandlerConfig.getByteOrder())
                            ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;

                    socketChannel.pipeline()
                            .addLast("tracerHandler", new TracerHandler())
                            .addLast("connectionLimitHandler", new ConnectionLimitHandler(parameter.protocolName(), tcpCfgHandler.getMaxConnections(), CHANNEL_GROUP, parameter.connectionsGauge()))
                            .addLast("idleStateHandler", new IdleStateHandler(tcpCfgHandler.getIdleTimeoutSeconds(), 0, 0))
                            .addLast("idleEventHandler", new IdleEventHandler(parameter.protocolName()));

                    if (LengthFieldBasedFrameDecoder.class.isAssignableFrom(binaryHandlerConfig.getDecoder())) {
                        LengthFieldBasedFrameDecoder framer = new LengthFieldBasedFrameDecoder(byteOrder,
                                binaryHandlerConfig.getMaxFrameLength(), binaryHandlerConfig.getLengthFieldOffset(),
                                binaryHandlerConfig.getLengthFieldLength(), binaryHandlerConfig.getLengthAdjustment(),
                                binaryHandlerConfig.getInitialBytesToStrip(), binaryHandlerConfig.isFailFast());
                        socketChannel.pipeline().addLast("LengthFieldBasedFrameDecoder", framer);
                    } else if (JCPPLengthFieldBasedFrameDecoder.class.isAssignableFrom(binaryHandlerConfig.getDecoder())) {
                        JCPPLengthFieldBasedFrameDecoder framer = new JCPPLengthFieldBasedFrameDecoder(binaryHandlerConfig.getHead(), byteOrder,
                                binaryHandlerConfig.getLengthFieldOffset(), binaryHandlerConfig.getLengthFieldLength(),
                                binaryHandlerConfig.getLengthAdjustment(), binaryHandlerConfig.getInitialBytesToStrip());
                        socketChannel.pipeline().addLast("JCPPLengthFieldBasedFrameDecoder", framer);
                    } else if (JCPPHeadTailFrameDecoder.class.isAssignableFrom(binaryHandlerConfig.getDecoder())) {
                        JCPPHeadTailFrameDecoder framer = new JCPPHeadTailFrameDecoder(binaryHandlerConfig.getHead(),
                                binaryHandlerConfig.getTail());
                        socketChannel.pipeline().addLast("JCPPHeadTailFrameDecoder", framer);
                    } else {
                        throw new IllegalArgumentException("未知的二进制解码器类型");
                    }

                    socketChannel.pipeline()
                            .addLast("tcpByteDecoderOverride", new TcpMsgDecoder<>(parameter.protocolName(), TcpMsgDecoder::toByteArray))
                            .addLast("tcpByteHandler", new TcpChannelHandler<>(parameter));
                }
            };
            case null -> throw new IllegalArgumentException("Unknown: " + tcpCfgHandler);
        };
    }
}
