/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.queue.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.service.PileProtocolService;
import sanbing.jcpp.app.service.queue.AbstractConsumerService;
import sanbing.jcpp.app.service.queue.AppConsumerStats;
import sanbing.jcpp.app.service.queue.AppQueueConsumerManager;
import sanbing.jcpp.infrastructure.queue.*;
import sanbing.jcpp.infrastructure.queue.common.QueueConfig;
import sanbing.jcpp.infrastructure.queue.common.TopicPartitionInfo;
import sanbing.jcpp.infrastructure.queue.discovery.PartitionProvider;
import sanbing.jcpp.infrastructure.queue.discovery.event.PartitionChangeEvent;
import sanbing.jcpp.infrastructure.queue.processing.IdMsgPair;
import sanbing.jcpp.infrastructure.queue.provider.AppQueueFactory;
import sanbing.jcpp.infrastructure.stats.StatsFactory;
import sanbing.jcpp.infrastructure.util.annotation.AppComponent;
import sanbing.jcpp.infrastructure.util.codec.ByteUtil;
import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerRunnable;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static sanbing.jcpp.infrastructure.queue.common.QueueConstants.*;


/**
 * @author baigod
 */
@Service
@AppComponent
@Slf4j
public class ProtocolUplinkConsumerService extends AbstractConsumerService implements ApplicationListener<PartitionChangeEvent> {

    @Value("${queue.app.poll-interval}")
    private int pollInterval;
    @Value("${queue.app.pack-processing-timeout}")
    private long packProcessingTimeout;
    @Value("${queue.app.consumer-per-partition}")
    private boolean consumerPerPartition;
    @Value("${queue.app.stats.enabled}")
    private boolean statsEnabled;

    private final PileProtocolService pileProtocolService;

    private final AppQueueFactory appQueueFactory;

    private AppQueueConsumerManager<ProtoQueueMsg<UplinkQueueMessage>, AppQueueConfig> appConsumer;

    private final AppConsumerStats stats;

    public ProtocolUplinkConsumerService(PartitionProvider partitionProvider,
                                         ApplicationEventPublisher eventPublisher,
                                         PileProtocolService pileProtocolService,
                                         AppQueueFactory appQueueFactory,
                                         StatsFactory statsFactory) {
        super(partitionProvider, eventPublisher);
        this.pileProtocolService = pileProtocolService;
        this.appQueueFactory = appQueueFactory;
        this.stats = new AppConsumerStats(statsFactory);
    }

    @PostConstruct
    public void init() {
        super.init("jcpp-app");

        log.info("Initializing Protocol Uplink Messages Queue Subscriptions.");

        this.appConsumer = AppQueueConsumerManager.<ProtoQueueMsg<UplinkQueueMessage>, AppQueueConfig>builder()
                .queueName("protocol uplink")
                .config(AppQueueConfig.of(consumerPerPartition, pollInterval))
                .msgPackProcessor(this::processMsgs)
                .consumerCreator((config, partitionId) -> appQueueFactory.createProtocolUplinkMsgConsumer())
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .build();
    }


    @Override
    @PreDestroy
    public void destroy() {
        super.destroy();
    }


    @Override
    protected void stopConsumers() {
        super.stopConsumers();
        appConsumer.stop();
        appConsumer.awaitStop();
    }


    @Scheduled(fixedDelayString = "${queue.app.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
            stats.reset();
        }
    }

    private void processMsgs(List<ProtoQueueMsg<UplinkQueueMessage>> msgs, QueueConsumer<ProtoQueueMsg<UplinkQueueMessage>> consumer, AppQueueConfig config) throws Exception {
        List<IdMsgPair<UplinkQueueMessage>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).toList();
        ConcurrentMap<UUID, ProtoQueueMsg<UplinkQueueMessage>> pendingMap = orderedMsgList.stream().collect(
                Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
        PackProcessingContext<ProtoQueueMsg<UplinkQueueMessage>> ctx = new PackProcessingContext<>(
                processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
        PendingMsgHolder pendingMsgHolder = new PendingMsgHolder();
        Future<?> packSubmitFuture = consumersExecutor.submit(new TracerRunnable(() ->
                orderedMsgList.forEach(element -> {

                    UUID id = element.getUuid();

                    ProtoQueueMsg<UplinkQueueMessage> msg = element.getMsg();

                    tracer(msg);

                    log.trace("[{}] Creating PackCallback for message: {}", id, msg.getValue());

                    Callback callback = new PackCallback<>(id, ctx);

                    try {
                        UplinkQueueMessage uplinkQueueMsg = msg.getValue();

                        if (statsEnabled) {
                            stats.log(uplinkQueueMsg);
                        }

                        pendingMsgHolder.setUplinkQueueMessage(uplinkQueueMsg);

                        if (uplinkQueueMsg.hasLoginRequest()) {

                            pileProtocolService.pileLogin(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasHeartBeatRequest()) {

                            pileProtocolService.heartBeat(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasVerifyPricingRequest()) {

                            pileProtocolService.verifyPricing(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasQueryPricingRequest()) {

                            pileProtocolService.queryPricing(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasGunRunStatusProto()) {

                            pileProtocolService.postGunRunStatus(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasChargingProgressProto()) {

                            pileProtocolService.postChargingProgress(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasSetPricingResponse()) {

                            pileProtocolService.onSetPricingResponse(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasRemoteStartChargingResponse()) {

                            pileProtocolService.onRemoteStartChargingResponse(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasRemoteStopChargingResponse()) {

                            pileProtocolService.onRemoteStopChargingResponse(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasTransactionRecordRequest()) {

                            pileProtocolService.onTransactionRecordRequest(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasBmsChargingErrorProto()) {

                            pileProtocolService.onBmsChargingErrorProto(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasBmsParamConfigReportProto()) {

                            pileProtocolService.onBmsParamConfigReport(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasBmsChargingInfoProto()) {

                            pileProtocolService.onBmsCharingInfo(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasBmsAbortProto()) {

                            pileProtocolService.postBmsAbort(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasRestartPileResponse()) {

                            pileProtocolService.onRestartPileResponse(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasBmsHandshakeProto()) {

                            pileProtocolService.postBmsHandshake(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasOtaResponse()) {

                            pileProtocolService.onOtaResponse(uplinkQueueMsg, callback);

                        } else if (uplinkQueueMsg.hasGroundLockStatusProto()) {

                            pileProtocolService.postLockStatus(uplinkQueueMsg, callback);
                        
                        } else {

                            callback.onSuccess();
                        }

                    } catch (Throwable e) {

                        log.warn("[{}] Failed to process message: {}", id, msg, e);

                        callback.onFailure(e);
                    }
                }))
        );

        if (!ctx.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {

            if (!packSubmitFuture.isDone()) {

                packSubmitFuture.cancel(true);

                UplinkQueueMessage lastSubmitMsg = pendingMsgHolder.getUplinkQueueMessage();

                log.warn("Timeout to process message: {}", lastSubmitMsg);
            }

            if (log.isDebugEnabled()) {

                ctx.getAckMap().forEach((id, msg) -> log.info("[{}] Timeout to process message: {}", id, msg.getValue()));

            }

            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
        }

        consumer.commit();
    }

    private void tracer(ProtoQueueMsg<UplinkQueueMessage> msg) {

        TracerContextUtil.newTracer(ByteUtil.bytesToString(msg.getHeaders().get(MSG_MD_TRACER_ID)),
                ByteUtil.bytesToString(msg.getHeaders().get(MSG_MD_TRACER_ORIGIN)),
                ByteUtil.bytesToLong(msg.getHeaders().get(MSG_MD_TRACER_TS)));

        MDCUtils.recordTracer();
    }

    @Override
    protected int getMgmtThreadPoolSize() {
        return Math.max(Runtime.getRuntime().availableProcessors(), 4);
    }

    @Override
    protected void onJCPPApplicationEvent(PartitionChangeEvent event) {
        Set<TopicPartitionInfo> appPartitions = event.getAppPartitions();
        log.info("Subscribing to partitions: {}", appPartitions);
        appConsumer.update(appPartitions);
    }

    @Data(staticConstructor = "of")
    public static class AppQueueConfig implements QueueConfig {
        private final boolean consumerPerPartition;
        private final int pollInterval;
    }

    @Setter
    @Getter
    private static class PendingMsgHolder {
        private UplinkQueueMessage uplinkQueueMessage;
    }
}