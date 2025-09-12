/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.listener.tcp;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.stats.MessagesStats;
import sanbing.jcpp.infrastructure.util.exception.DownlinkException;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.DownlinkProto.DownlinkRequestMessage;
import sanbing.jcpp.proto.gen.UplinkProto.SessionCloseReason;
import sanbing.jcpp.protocol.ProtocolMessageProcessor;
import sanbing.jcpp.protocol.domain.ListenerToHandlerMsg;
import sanbing.jcpp.protocol.domain.ProtocolUplinkMsg;
import sanbing.jcpp.protocol.domain.SessionToHandlerMsg;
import sanbing.jcpp.protocol.listener.ChannelHandlerParameter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
public class TcpChannelHandler<T> extends SimpleChannelInboundHandler<ProtocolUplinkMsg<T>> {
    private final String protocolName;
    private final ProtocolMessageProcessor protocolMessageProcessor;

    private final MessagesStats uplinkMsgStats;
    private final MessagesStats downlinkMsgStats;
    private final Timer downlinkTimer;

    private final TcpSession tcpSession;

    @SneakyThrows
    public TcpChannelHandler(ChannelHandlerParameter parameter) {
        this.protocolName = parameter.protocolName();
        this.protocolMessageProcessor = parameter.protocolMessageProcessor();

        this.uplinkMsgStats = parameter.uplinkMsgStats();
        this.downlinkMsgStats = parameter.downlinkMsgStats();
        this.downlinkTimer = parameter.downlinkTimer();

        tcpSession = new TcpSession(protocolName, this::onDownlink, this::writeAndFlush);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolUplinkMsg<T> msg) {

        if (log.isDebugEnabled()) {

            log.debug("[{}]{}{} Netty拆出上行报文:{}", protocolName, ctx.channel(), tcpSession, msg);
        }

        uplinkMsgStats.incrementTotal();

        tcpSession.setLastActivityTime(LocalDateTime.now());

        if (tcpSession.getAddress() == null) {

            tcpSession.setAddress(msg.address());
        }

        if (tcpSession.getCtx() == null) {

            tcpSession.setCtx(ctx);
        }

        T data = msg.data();

        if (Objects.isNull(data)) {

            log.debug("[{}]{}{} 上行报文为空被过滤 [{}]", protocolName, ctx.channel(), tcpSession, msg);

            return;
        }

        try {

            process(msg, ctx);

            uplinkMsgStats.incrementSuccessful();

        } catch (Exception e) {

            uplinkMsgStats.incrementFailed();

            log.error("[{}]{}{} TCP管道处理报文异常", protocolName, ctx.channel(), tcpSession, e);
        }
    }

    private void process(ProtocolUplinkMsg<T> msg, ChannelHandlerContext ctx) {
        switch (msg.data()) {
            case byte[] bytes ->
                    protocolMessageProcessor.uplinkHandleAsync(new ListenerToHandlerMsg(msg.id(), bytes, tcpSession), uplinkMsgStats);
            case JsonNode json ->
                    protocolMessageProcessor.uplinkHandleAsync(new ListenerToHandlerMsg(msg.id(), JacksonUtil.writeValueAsBytes(json), tcpSession), uplinkMsgStats);
            case String text ->
                    protocolMessageProcessor.uplinkHandleAsync(new ListenerToHandlerMsg(msg.id(), JacksonUtil.writeValueAsBytes(text.getBytes()), tcpSession), uplinkMsgStats);
            case null, default -> {
                assert msg.data() != null;
                log.warn("[{}]{}{} 不支持的TCP上行报文类型:{}", protocolName, ctx.channel(), tcpSession, msg.data().getClass());
            }
        }
    }

    protected void onDownlink(DownlinkRequestMessage downlinkMsg) throws DownlinkException {
        protocolMessageProcessor.downlinkHandle(new SessionToHandlerMsg(downlinkMsg, tcpSession), downlinkMsgStats);
    }

    protected void writeAndFlush(ByteBuf... byteBufList) {
        if (byteBufList == null || byteBufList.length == 0) {

            return;
        }

        ChannelHandlerContext ctx = tcpSession.getCtx();

        if (ctx.isRemoved()) {

            tcpSession.close(SessionCloseReason.SESSION_CLOSE_ON_CHANNEL_INACTIVE);

            log.warn("[{}]{}{} TCP会话已失效，因此删除会话", protocolName, ctx.channel(), tcpSession);

            return;
        }

        downlinkMsgStats.incrementTotal(byteBufList.length);

        for (ByteBuf byteBuf : byteBufList) {

            try {

                if (Objects.isNull(byteBuf)) {
                    log.warn("[{}]{}{} 下发空报文被拦截", protocolName, ctx.channel(), tcpSession);
                    continue;
                }

                logDownlinkStart(() -> ByteBufUtil.hexDump(byteBuf));

                ctx.writeAndFlush(Unpooled.wrappedBuffer(byteBuf))
                        .addListener(this::logDownlinkUnsuccessful);


                downlinkMsgStats.incrementSuccessful();

            } catch (Exception e) {

                downlinkMsgStats.incrementFailed();

                throw e;
            }
        }

    }

    private void logDownlinkStart(Supplier<String> logTransform) {

        if (log.isInfoEnabled()) {
            log.info("[{}]{} 开始发送下行报文:{}", protocolName, tcpSession, logTransform.get());
        }
    }

    private void logDownlinkUnsuccessful(Future<? super Void> channelFuture) {

        downlinkTimer.record(Duration.ofMillis(System.currentTimeMillis() - TracerContextUtil.getCurrentTracer().getTracerTs()));

        if (channelFuture.isDone() && !channelFuture.isSuccess()) {
            log.info("[{}]{} 下行报文发送未成功", protocolName, tcpSession);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {

        ctx.flush();

        if (log.isTraceEnabled()) {
            log.trace("[{}]{}{} Channel Read Complete [{}]", protocolName, ctx.channel(), tcpSession, ctx.name());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[{}]{}{} Invalid message received, Exception caught", protocolName, ctx.channel(), tcpSession, cause);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

        super.channelRegistered(ctx);

        log.info("[{}]{} 打开通道", protocolName, ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

        super.channelUnregistered(ctx);

        log.info("[{}]{}{} 关闭通道", protocolName, ctx.channel(), tcpSession);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        super.channelActive(ctx);

        log.info("[{}]{} 通道活跃", protocolName, ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        super.channelInactive(ctx);

        // 处理连接断开逻辑
        tcpSession.close(SessionCloseReason.SESSION_CLOSE_ON_CHANNEL_INACTIVE);

        log.info("[{}]{}{} 通道不活跃，连接已断开", protocolName, ctx.channel(), tcpSession);
    }
}