/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.listener.tcp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class ConnectionLimitHandler extends ChannelInboundHandlerAdapter {
    private final String protocolName;
    private final int maxConnections;
    private final ChannelGroup channelGroup;
    private final AtomicInteger connectionsGauge;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (connectionsGauge.incrementAndGet() > maxConnections) {
            ctx.close();
            log.info("[{}]{} channelRegistered超过最大连接数 {}，因此关闭连接 {}",protocolName, ctx.channel(), maxConnections, ctx.channel());
        } else {
            super.channelRegistered(ctx);
            log.info("[{}]{} channelRegistered 当前连接数 {} / {}",protocolName, ctx.channel(), connectionsGauge.get(), maxConnections);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        connectionsGauge.decrementAndGet();
        super.channelUnregistered(ctx);
        log.info("[{}]{} channelUnregistered 当前连接数 {} / {}",protocolName, ctx.channel(), connectionsGauge.get(), maxConnections);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        channelGroup.add(ctx.channel());
        log.info("[{}]{} channelActive 当前连接数 {} / {}",protocolName, ctx.channel(), channelGroup.size(), maxConnections);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        channelGroup.remove(ctx.channel());
        log.info("[{}]{} channelInactive 当前连接数 {} / {}",protocolName, ctx.channel(), channelGroup.size(), maxConnections);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[{}]{} ConnectionLimitHandler exceptionCaught",protocolName, ctx.channel(), cause);
        ctx.close();
    }
}
