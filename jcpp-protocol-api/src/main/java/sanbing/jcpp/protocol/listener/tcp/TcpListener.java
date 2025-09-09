/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.listener.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.async.JCPPThreadFactory;
import sanbing.jcpp.protocol.ProtocolMessageProcessor;
import sanbing.jcpp.protocol.cfg.TcpCfg;
import sanbing.jcpp.protocol.listener.ChannelHandlerInitializer;
import sanbing.jcpp.protocol.listener.Listener;

/**
 * @author 九筒
 */
@Slf4j
public class TcpListener extends Listener {

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public TcpListener(String protocolName, TcpCfg tcpCfg, ProtocolMessageProcessor protocolMessageProcessor, StatsFactory statsFactory) throws InterruptedException {
        super(protocolName, protocolMessageProcessor, statsFactory);

        tcpServerBootstrap(tcpCfg);
    }

    private void tcpServerBootstrap(TcpCfg tcpCfg) throws InterruptedException {
        bossGroup = new NioEventLoopGroup(tcpCfg.getBossGroupThreadCount(), JCPPThreadFactory.forName("tcp-boss-%d"));
        workerGroup = new NioEventLoopGroup(tcpCfg.getWorkerGroupThreadCount(), JCPPThreadFactory.forName("tcp-worker-%d"));

        ChannelHandlerInitializer<SocketChannel> channelHandler = ChannelHandlerInitializer.createTcpChannelHandler(tcpCfg, parameter);

        ServerBootstrap server = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, tcpCfg.getSoBacklog())
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_RCVBUF, tcpCfg.getSoRcvbuf())
                .childOption(ChannelOption.SO_KEEPALIVE, tcpCfg.isSoKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, tcpCfg.isNodelay())
                .childOption(ChannelOption.SO_SNDBUF, tcpCfg.getSoSndbuf())
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(channelHandler);
        serverChannel = server.bind(tcpCfg.getBindAddress(), tcpCfg.getBindPort()).sync().channel();
        log.info("Tcp server [{}] started, BindAddress:[{}], BindPort: [{}]", getProtocolName(), tcpCfg.getBindAddress(), tcpCfg.getBindPort());
    }


    private void tcpServerShutdown() throws InterruptedException {
        if (this.serverChannel != null) {
            ChannelFuture cf = this.serverChannel.close().sync();
            cf.awaitUninterruptibly();
        }

        Future<?> bossFuture = null;
        Future<?> workerFuture = null;

        if (bossGroup != null) {
            bossFuture = bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerFuture = workerGroup.shutdownGracefully();
        }

        log.info("[{}] Awaiting shutdown gracefully boss and worker groups...", getProtocolName());

        if (bossFuture != null) {
            bossFuture.sync();
        }
        if (workerFuture != null) {
            workerFuture.sync();
        }

        log.info("[{}] Protocol server stopped!", getProtocolName());
    }

    @Override
    public Health health() {
        if (serverChannel != null) {
            if (serverChannel.isActive()) {
                return Health.up().withDetail("TcpServer", "Active").build();
            } else {
                return Health.down().withDetail("TcpServer", "Inactive").build();
            }
        }
        return Health.down().build();
    }

    @Override
    public void destroy() throws InterruptedException {
        tcpServerShutdown();
    }
}