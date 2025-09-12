/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.grpc;

import com.google.common.net.HostAndPort;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceInfoProvider;
import sanbing.jcpp.infrastructure.util.async.JCPPThreadFactory;
import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerRunnable;
import sanbing.jcpp.proto.gen.DownlinkProto.DownlinkResponseMessage;
import sanbing.jcpp.proto.gen.GrpcProto.*;
import sanbing.jcpp.proto.gen.ProtocolInterfaceGrpc;
import sanbing.jcpp.proto.gen.ProtocolInterfaceGrpc.ProtocolInterfaceStub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static sanbing.jcpp.infrastructure.proto.ProtoConverter.toTracerProto;

/**
 * @author 九筒
 */
@Component
@Slf4j
public class DownlinkGrpcClient {

    @Value("${service.downlink.rpc.grpc.netty.event_loop:}")
    private Integer rpcNettyEventLoop;

    @Value("${service.downlink.rpc.grpc.netty.so_sndbuf:65535}")
    private Integer rpcNettySoSndbuf;

    @Value("${service.downlink.rpc.grpc.netty.so_rcvbuf:65535}")
    private Integer rpcNettySoRcvbuf;

    @Value("${service.downlink.rpc.grpc.netty.no_delay:true}")
    private boolean rpcNoDelay;

    @Value("${service.downlink.rpc.grpc.netty.max_inbound_message_size:33554432}")
    private Integer rpcMaxInboundMessageSize;

    @Value("${service.downlink.rpc.grpc.keep_alive_time_sec:300}")
    private int keepAliveTimeSec;

    @Value("${service.downlink.rpc.grpc.max_records_size:102400}")
    private int maxRecordsSize;

    @Value("${service.downlink.rpc.grpc.batch_records_count:1024}")
    private int batchRecordsCount;

    @Value("${service.downlink.rpc.grpc.no_read_records_sleep:25}")
    private long noRecordsSleepInterval;

    @Value("${service.downlink.rpc.grpc.records_ttl:600000}")
    private long recordsTtl;

    @Value("${service.downlink.rpc.grpc.max_reconnect_times:10}")
    private int maxReconnectTimes;

    @Resource
    ServiceInfoProvider serviceInfoProvider;

    private final Map<HostAndPort, ManagedChannel> channelMap = new ConcurrentHashMap<>();
    private final Map<HostAndPort, StreamObserver<RequestMsg>> inputStreamMap = new ConcurrentHashMap<>();
    private final Map<HostAndPort, LinkedBlockingQueue<RequestMsg>> queueMap = new ConcurrentHashMap<>();
    private final Map<HostAndPort, ReentrantLock> msgHandleLocksMap = new ConcurrentHashMap<>();
    private final Map<HostAndPort, ExecutorService> msgHandleExecutorMap = new ConcurrentHashMap<>();
    private final Map<HostAndPort, AtomicInteger> connectErrTimesMap = new ConcurrentHashMap<>();
    private final Map<HostAndPort, Boolean> initializedMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService downlinkMsgsExecutor;

    @PostConstruct
    public void init() {
        downlinkMsgsExecutor = Executors.newSingleThreadScheduledExecutor(JCPPThreadFactory.forName("downlink-msgs-executor"));

        // 每秒进行一次连接检查与线程初始化
        downlinkMsgsExecutor.scheduleWithFixedDelay(() -> {
            queueMap.forEach((key, queue) -> {

                ManagedChannel managedChannel = channelMap.get(key);

                if (managedChannel == null) {
                    connect(key);
                }

                msgHandleExecutorMap.computeIfAbsent(key, hostAndPort ->
                                Executors.newFixedThreadPool(1, JCPPThreadFactory.forName("downlink-handle-threads-" + hostAndPort)))
                        .execute(new TracerRunnable(() -> {
                            while (initializedMap.computeIfAbsent(key, k -> Boolean.FALSE)) {
                                try {
                                    handleMsgs(key, queue);
                                } catch (Exception e) {
                                    log.error("Failed to process messages handling!", e);
                                }
                            }
                        }));
            });

        }, 0, 1, TimeUnit.SECONDS);
    }

    private void handleMsgs(HostAndPort key, LinkedBlockingQueue<RequestMsg> queue) {
        StreamObserver<RequestMsg> inputStream = inputStreamMap.get(key);

        if (inputStream == null) {
            return;
        }

        long acceptTs = System.currentTimeMillis() - recordsTtl;

        List<RequestMsg> downlinkMsgs = new ArrayList<>(batchRecordsCount);

        queue.drainTo(downlinkMsgs, batchRecordsCount);

        for (RequestMsg msg : downlinkMsgs) {

            long ts = msg.getTs();

            if (ts > 0 && ts < acceptTs) {

                log.warn("[{}] 消息过期，直接丢弃 {}", key, ts);

                continue;
            }


            ReentrantLock lock = msgHandleLocksMap.computeIfAbsent(key, hostAndPort -> new ReentrantLock());

            lock.lock();
            try {
                inputStream.onNext(msg);
            } finally {
                lock.unlock();
            }
        }

        if (downlinkMsgs.isEmpty()) {
            try {
                Thread.sleep(noRecordsSleepInterval);
            } catch (InterruptedException e) {
                log.warn("Sleep interrupted!", e);
            }
        } else {
            downlinkMsgs.clear();
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Starting Grpc destroying process");

        initializedMap.replaceAll((hostAndPort, aBoolean) -> Boolean.FALSE);

        try {
            downlinkMsgsExecutor.shutdownNow();

            msgHandleExecutorMap.values().forEach(ExecutorService::shutdownNow);

            inputStreamMap.values().forEach(StreamObserver::onCompleted);

            channelMap.values().forEach(ManagedChannel::shutdownNow);

        } catch (Exception e) {
            log.error("Exception during disconnect", e);
        }
    }

    public void connect(HostAndPort hostAndPort) {

        log.info("[{}] Create new Grpc Client Channel!", hostAndPort);

        ManagedChannel managedChannel = NettyChannelBuilder.forAddress(hostAndPort.getHost(), hostAndPort.getPort())
                .eventLoopGroup(new NioEventLoopGroup(Optional.ofNullable(rpcNettyEventLoop).orElse(Runtime.getRuntime().availableProcessors() * 2)))
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                .withOption(ChannelOption.SO_SNDBUF, rpcNettySoSndbuf)
                .withOption(ChannelOption.SO_RCVBUF, rpcNettySoRcvbuf)
                .withOption(ChannelOption.TCP_NODELAY, rpcNoDelay)
                .maxInboundMessageSize(rpcMaxInboundMessageSize)
                .channelType(NioSocketChannel.class)
                .directExecutor()
                .keepAliveTime(keepAliveTimeSec, TimeUnit.SECONDS)
                .usePlaintext()
                .keepAliveTimeout(10, TimeUnit.SECONDS) // Change to a larger value, e.g. 10s.
                .keepAliveWithoutCalls(true)// You should normally avoid enabling this.
                .defaultLoadBalancingPolicy("round_robin")
                .build();

        ManagedChannel remove = channelMap.remove(hostAndPort);

        if (remove != null) {
            channelMap.get(hostAndPort).shutdownNow();
        }

        channelMap.put(hostAndPort, managedChannel);

        ProtocolInterfaceStub stub = ProtocolInterfaceGrpc.newStub(managedChannel);

        StreamObserver<RequestMsg> streamObserver = stub.onDownlink(new StreamObserver<>() {
            @Override
            public void onNext(ResponseMsg responseMsg) {
                TracerProto tracerProto = responseMsg.getTracer();
                TracerContextUtil.newTracer(tracerProto.getId(), tracerProto.getOrigin(), tracerProto.getTs());
                MDCUtils.recordTracer();

                if (responseMsg.hasConnectResponseMsg()) {
                    log.info("[{}] Grpc 接收到通信层连接反馈 {}", hostAndPort, responseMsg.getConnectResponseMsg());
                    if (ConnectResponseCode.ACCEPTED.equals(responseMsg.getConnectResponseMsg().getResponseCode())) {

                        initializedMap.put(hostAndPort, Boolean.TRUE);

                    } else {
                        onError(new RuntimeException(responseMsg.getConnectResponseMsg().getErrorMsg()));
                    }
                }

                if (responseMsg.hasDownlinkResponseMsg()) {
                    DownlinkResponseMessage downlinkResponseMsg = responseMsg.getDownlinkResponseMsg();
                    if (!downlinkResponseMsg.getSuccess()) {
                        log.info("[{}] Grpc 下行数据发生错误回复 {}", hostAndPort, downlinkResponseMsg);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("[{}] Grpc 客户端异常 {}", hostAndPort, t.getMessage(), t);

                ExecutorService executorService = msgHandleExecutorMap.get(hostAndPort);
                if (executorService != null) {
                    executorService.shutdownNow();
                    msgHandleExecutorMap.remove(hostAndPort);
                }

                ManagedChannel remove = channelMap.remove(hostAndPort);

                if (remove != null) {
                    remove.shutdownNow();
                }

                if (connectErrTimesMap.computeIfAbsent(hostAndPort, k -> new AtomicInteger()).incrementAndGet() >= maxReconnectTimes) {
                    LinkedBlockingQueue<RequestMsg> queue = queueMap.remove(hostAndPort);
                    if (queue != null) {
                        queue.clear();
                    }
                    connectErrTimesMap.remove(hostAndPort);
                    log.info("[{}] Grpc 客户端重连异常超过{}次，不再重连", hostAndPort, maxReconnectTimes);
                }
            }

            @Override
            public void onCompleted() {
                log.info("[{}] The Grpc connection was closed!", hostAndPort);
            }
        });

        streamObserver.onNext(RequestMsg.newBuilder()
                .setTracer(toTracerProto())
                .setConnectRequestMsg(ConnectRequestMsg.newBuilder()
                        .setNodeId(serviceInfoProvider.getServiceId())
                        .build())
                .build());

        inputStreamMap.put(hostAndPort, streamObserver);

    }

    /**
     * 发送下行请求
     */
    public void sendDownlinkRequest(HostAndPort hostAndPort, RequestMsg requestMsg) {
        queueMap.computeIfAbsent(hostAndPort, k -> new LinkedBlockingQueue<>(maxRecordsSize)).add(requestMsg);
    }
}