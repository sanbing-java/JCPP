/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.adapter;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerRunnable;
import sanbing.jcpp.proto.gen.ProtocolInterfaceGrpc.ProtocolInterfaceImplBase;
import sanbing.jcpp.proto.gen.ProtocolProto.*;
import sanbing.jcpp.protocol.domain.ProtocolSession;
import sanbing.jcpp.protocol.provider.ProtocolSessionRegistryProvider;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static sanbing.jcpp.infrastructure.proto.ProtoConverter.toTracerProto;
import static sanbing.jcpp.infrastructure.util.config.ThreadPoolConfiguration.JCPP_COMMON_THREAD_POOL;

/**
 * @author 九筒
 */
@Service
@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='protocol'")
public class DownlinkGrpcService extends ProtocolInterfaceImplBase {
    @Value("${service.protocol.grpc.port}")
    private int grpcPort;
    @Value("${service.protocol.grpc.boss}")
    private int grpcBoss;
    @Value("${service.protocol.grpc.worker}")
    private int grpcWorker;
    @Value("${service.protocol.grpc.so-rcvbuf}")
    private int grpcNettySoRcvbuf;
    @Value("${service.protocol.grpc.so-sndbuf}")
    private int grpcNettySoSndbuf;
    @Value("${service.protocol.grpc.no-delay}")
    private boolean grpcNettyNoDelay;
    @Value("${service.protocol.grpc.max-inbound-message-size}")
    private int maxInboundMessageSize;
    @Value("${service.protocol.grpc.max-concurrent-calls-per-connection}")
    private int maxConcurrentCallsPerConnection;
    @Value("${service.protocol.grpc.client-max-keep-alive-time-sec}")
    private int clientMaxKeepAliveTimeSec;

    @Resource
    ProtocolSessionRegistryProvider protocolSessionRegistryProvider;

    private Server server;
    private static final ReentrantLock replyLock = new ReentrantLock();

    @PostConstruct
    public void init() throws Exception {
        log.info("Initializing Protocol Downlink Grpc service!");

        NettyServerBuilder builder = NettyServerBuilder.forPort(this.grpcPort)
                .bossEventLoopGroup(new NioEventLoopGroup(this.grpcBoss))
                .workerEventLoopGroup(new NioEventLoopGroup(this.grpcWorker))
                .withOption(ChannelOption.SO_RCVBUF, grpcNettySoRcvbuf)
                .withChildOption(ChannelOption.SO_SNDBUF, grpcNettySoSndbuf)
                .withChildOption(ChannelOption.TCP_NODELAY, grpcNettyNoDelay)
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                .channelType(NioServerSocketChannel.class)
                .permitKeepAliveTime(this.clientMaxKeepAliveTimeSec, TimeUnit.SECONDS)
                .maxInboundMessageSize(maxInboundMessageSize)
                .maxConcurrentCallsPerConnection(maxConcurrentCallsPerConnection)
                .directExecutor()
                .keepAliveTime(5, TimeUnit.MINUTES)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true)
                .addService(this);

        this.server = builder.build();
        log.info("Going to start RPC server using port: {}", this.grpcPort);

        try {
            this.server.start();
        } catch (Exception e) {
            log.error("Failed to start RPC server!", e);
            throw e;
        }

        log.info("Protocol Downlink Grpc service initialized!");
    }

    @PreDestroy
    public void destroy() {
        if (this.server != null) {
            this.server.shutdownNow();
        }
    }

    @Override
    public StreamObserver<RequestMsg> onDownlink(StreamObserver<ResponseMsg> responseObserver) {
        return new StreamObserver<>() {

            @Override
            public void onNext(RequestMsg requestMsg) {
                TracerProto tracerProto = requestMsg.getTracer();
                TracerContextUtil.newTracer(tracerProto.getId(), tracerProto.getOrigin(), tracerProto.getTs());
                MDCUtils.recordTracer();

                log.info("通信层收到Grpc下行请求 {}", requestMsg);

                if (requestMsg.hasConnectRequestMsg()) {
                    replyLock.lock();
                    try {
                        responseObserver.onNext(
                                ResponseMsg.newBuilder()
                                        .setTracer(toTracerProto())
                                        .setConnectResponseMsg(ConnectResponseMsg.newBuilder()
                                                .setResponseCode(ConnectResponseCode.ACCEPTED)
                                                .setErrorMsg("")
                                                .build())
                                        .build());
                    } finally {
                        replyLock.unlock();
                    }
                }

                if(requestMsg.hasDownlinkRequestMessage()){
                    DownlinkRequestMessage downlinkMsg = requestMsg.getDownlinkRequestMessage();
                    JCPP_COMMON_THREAD_POOL.execute(new TracerRunnable(() -> {
                        UUID protocolSessionId = new UUID(downlinkMsg.getSessionIdMSB(), downlinkMsg.getSessionIdLSB());

                        ProtocolSession protocolSession = protocolSessionRegistryProvider.get(protocolSessionId);

                        try {
                            if (protocolSession != null) {

                                protocolSession.onDownlink(downlinkMsg);

                            } else {

                                log.info("下发报文时Session未找到 sessionId: {}", protocolSessionId);

                            }
                        } catch (Exception e) {

                            log.warn("下发报文时处理失败 sessionId: {}", protocolSessionId, e);

                        }
                    }));
                }


            }

            @Override
            public void onError(Throwable t) {
                log.error("Failed to deliver message from client!", t);
            }

            @Override
            public void onCompleted() {
                try {
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    log.error("onCompleted error ", e);
                }
            }
        };
    }
}