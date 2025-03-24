/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.data.PileSession;
import sanbing.jcpp.app.repository.PileRepository;
import sanbing.jcpp.app.service.DownlinkCallService;
import sanbing.jcpp.app.service.PileProtocolService;
import sanbing.jcpp.app.service.cache.session.PileSessionCacheKey;
import sanbing.jcpp.infrastructure.cache.TransactionalCache;
import sanbing.jcpp.infrastructure.proto.ProtoConverter;
import sanbing.jcpp.infrastructure.proto.model.PricingModel;
import sanbing.jcpp.infrastructure.proto.model.PricingModel.FlagPrice;
import sanbing.jcpp.infrastructure.proto.model.PricingModel.Period;
import sanbing.jcpp.infrastructure.queue.Callback;
import sanbing.jcpp.proto.gen.ProtocolProto.*;
import sanbing.jcpp.protocol.domain.DownlinkCmdEnum;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

import static sanbing.jcpp.proto.gen.ProtocolProto.PricingModelFlag.*;
import static sanbing.jcpp.proto.gen.ProtocolProto.PricingModelRule.SPLIT_TIME;
import static sanbing.jcpp.proto.gen.ProtocolProto.PricingModelType.CHARGE;

/**
 * @author baigod
 */
@Service
@Slf4j
public class DefaultPileProtocolService implements PileProtocolService {

    @Resource
    PileRepository pileRepository;

    @Resource
    TransactionalCache<PileSessionCacheKey, PileSession> pileSessionCache;

    @Resource
    DownlinkCallService downlinkCallService;

    @Override
    public void pileLogin(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.debug("接收到桩登录事件 {}", uplinkQueueMessage);

        LoginRequest loginRequest = uplinkQueueMessage.getLoginRequest();

        Pile pile = pileRepository.findPileByCode(loginRequest.getPileCode());

        String pileCode = loginRequest.getPileCode();

        log.debug("查询到充电桩信息 {}", pile);

        // 构造下行回复
        DownlinkRequestMessage.Builder downlinkMessageBuilder = createDownlinkMessageBuilder(uplinkQueueMessage, loginRequest.getPileCode());
        downlinkMessageBuilder.setDownlinkCmd(DownlinkCmdEnum.LOGIN_ACK.name());


        if (pile != null) {

            PileSession pileSession = createSession(uplinkQueueMessage, pile,
                    loginRequest.getRemoteAddress(),
                    loginRequest.getNodeId(),
                    loginRequest.getNodeHostAddress(),
                    loginRequest.getNodeRestPort(),
                    loginRequest.getNodeGrpcPort());

            // 保存到缓存
            pileSessionCache.put(new PileSessionCacheKey(pile.getPileCode()), pileSession);

            downlinkMessageBuilder.setLoginResponse(LoginResponse.newBuilder()
                    .setSuccess(true)
                    .setPileCode(loginRequest.getPileCode())
                    .build());

            downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, pileCode);
        } else {

            downlinkMessageBuilder.setLoginResponse(LoginResponse.newBuilder()
                    .setSuccess(false)
                    .setPileCode(loginRequest.getPileCode())
                    .build());


            downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, uplinkQueueMessage, loginRequest);
        }


        callback.onSuccess();
    }

    @Override
    public void heartBeat(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.debug("接收到桩心跳事件 {}", uplinkQueueMessage);

        HeartBeatRequest heartBeatRequest = uplinkQueueMessage.getHeartBeatRequest();

        Pile pile = pileRepository.findPileByCode(heartBeatRequest.getPileCode());

        if (pile != null) {
            // 重新保存到缓存
            createSession(uplinkQueueMessage, pile,
                    heartBeatRequest.getRemoteAddress(),
                    heartBeatRequest.getNodeId(),
                    heartBeatRequest.getNodeHostAddress(),
                    heartBeatRequest.getNodeRestPort(),
                    heartBeatRequest.getNodeGrpcPort());
        }

        callback.onSuccess();
    }

    private PileSession createSession(UplinkQueueMessage uplinkQueueMessage,
                                      Pile pile,
                                      String remoteAddress,
                                      String nodeId,
                                      String nodeIp,
                                      int restPort,
                                      int grpcPort) {
        PileSession pileSession = new PileSession(pile.getId(), pile.getPileCode(), uplinkQueueMessage.getProtocolName());
        pileSession.setProtocolSessionId(new UUID(uplinkQueueMessage.getSessionIdMSB(), uplinkQueueMessage.getSessionIdLSB()));
        pileSession.setRemoteAddress(remoteAddress);
        pileSession.setNodeId(nodeId);
        pileSession.setNodeIp(nodeIp);
        pileSession.setNodeRestPort(restPort);
        pileSession.setNodeGrpcPort(grpcPort);

        return pileSession;
    }

    @Override
    public void verifyPricing(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到计费模型验证请求 {}", uplinkQueueMessage);

        VerifyPricingRequest verifyPricingRequest = uplinkQueueMessage.getVerifyPricingRequest();
        String pileCode = verifyPricingRequest.getPileCode();

        long pricingId = verifyPricingRequest.getPricingId();
        // todo 默认校验成功，后续查库校验
        assert pricingId > 0;

        DownlinkRequestMessage.Builder downlinkMessageBuilder = createDownlinkMessageBuilder(uplinkQueueMessage, pileCode);
        downlinkMessageBuilder.setDownlinkCmd(DownlinkCmdEnum.VERIFY_PRICING_ACK.name());
        downlinkMessageBuilder.setVerifyPricingResponse(VerifyPricingResponse.newBuilder()
                .setSuccess(true)
                .setPricingId(pricingId)
                .build());

        downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, pileCode);

        callback.onSuccess();
    }

    @Override
    public void queryPricing(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩计费模型请求 {}", uplinkQueueMessage);

        QueryPricingRequest queryPricingRequest = uplinkQueueMessage.getQueryPricingRequest();
        String pileCode = queryPricingRequest.getPileCode();

        // TODO 先构造一个通用的计费模型，后续根据业务做库查询
        List<Period> periods = new ArrayList<>();

        periods.add(createPeriod(1, LocalTime.parse("00:00"), LocalTime.parse("06:00"), TOP));
        periods.add(createPeriod(2, LocalTime.parse("06:00"), LocalTime.parse("12:00"), PEAK));
        periods.add(createPeriod(3, LocalTime.parse("12:00"), LocalTime.parse("18:00"), FLAT));
        periods.add(createPeriod(4, LocalTime.parse("18:00"), LocalTime.parse("00:00"), VALLEY));

        Map<PricingModelFlag, FlagPrice> flagPriceMap = new HashMap<>();
        flagPriceMap.put(TOP, new FlagPrice(new BigDecimal("0.75"), new BigDecimal("0.45")));
        flagPriceMap.put(PEAK, new FlagPrice(new BigDecimal("0.75"), new BigDecimal("0.45")));
        flagPriceMap.put(FLAT, new FlagPrice(new BigDecimal("0.75"), new BigDecimal("0.45")));
        flagPriceMap.put(VALLEY, new FlagPrice(new BigDecimal("0.75"), new BigDecimal("0.45")));

        PricingModel model = new PricingModel();
        model.setId(UUID.randomUUID());
        model.setSequenceNumber(1);
        model.setPileCode(pileCode);
        model.setType(CHARGE);
        model.setRule(SPLIT_TIME);
        model.setStandardElec(new BigDecimal("0.75"));
        model.setStandardServ(new BigDecimal("0.45"));
        model.setFlagPriceList(flagPriceMap);
        model.setPeriodsList(periods);

        // 构造下行计费
        DownlinkRequestMessage.Builder downlinkMessageBuilder = createDownlinkMessageBuilder(uplinkQueueMessage, pileCode);
        downlinkMessageBuilder.setDownlinkCmd(DownlinkCmdEnum.QUERY_PRICING_ACK.name());
        downlinkMessageBuilder.setQueryPricingResponse(QueryPricingResponse.newBuilder()
                .setPileCode(pileCode)
                .setPricingId(model.getSequenceNumber())
                .setPricingModel(ProtoConverter.toPricingModel(model))
                .build());

        downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, pileCode);

        callback.onSuccess();
    }

    @Override
    public void postGunRunStatus(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩上报的电桩状态 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void postChargingProgress(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩上报的充电进度 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void onSetPricingResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩上费率下发反馈 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void onRemoteStartChargingResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩启动结果反馈 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void onRemoteStopChargingResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩停止结果反馈 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void onTransactionRecord(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩交易记录上报 {}", uplinkQueueMessage);

        // todo 毛都不敢先给个回复
        TransactionRecord transactionRecord = uplinkQueueMessage.getTransactionRecord();

        String tradeNo = transactionRecord.getTradeNo();
        String pileCode = transactionRecord.getPileCode();

        // 构造下行计费
        DownlinkRequestMessage.Builder downlinkMessageBuilder = createDownlinkMessageBuilder(uplinkQueueMessage, pileCode);
        downlinkMessageBuilder.setDownlinkCmd(DownlinkCmdEnum.TRANSACTION_RECORD.name());
        downlinkMessageBuilder.setTransactionRecordAck(TransactionRecordAck.newBuilder()
                .setTradeNo(tradeNo)
                .setSuccess(true)
                .build());

        downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, pileCode);

        callback.onSuccess();
    }

    @Override
    public void startCharge(String pileCode, String gunCode, BigDecimal limitYuan, String orderNo) {


        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(pileCode)
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.REMOTE_START_CHARGING.name())
                .setRemoteStartChargingRequest(RemoteStartChargingRequest.newBuilder()
                        .setPileCode(pileCode)
                        .setGunCode(gunCode)
                        .setLimitYuan(limitYuan.toPlainString())
                        .setTradeNo(orderNo)
                        .build());


        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder, pileCode);
    }

    private static Period createPeriod(int sn, LocalTime beginTime, LocalTime endTime, PricingModelFlag flag) {
        Period period = new Period();
        period.setSn(sn);
        period.setBegin(beginTime);
        period.setEnd(endTime);
        period.setFlag(flag);
        return period;
    }

    private DownlinkRequestMessage.Builder createDownlinkMessageBuilder(UplinkQueueMessage uplinkQueueMessage, String pileCode) {
        UUID messageId = UUID.randomUUID();
        DownlinkRequestMessage.Builder builder = DownlinkRequestMessage.newBuilder();
        builder.setMessageIdMSB(messageId.getLeastSignificantBits());
        builder.setMessageIdLSB(messageId.getLeastSignificantBits());
        builder.setPileCode(pileCode);
        builder.setSessionIdMSB(uplinkQueueMessage.getSessionIdMSB());
        builder.setSessionIdLSB(uplinkQueueMessage.getSessionIdLSB());
        builder.setProtocolName(uplinkQueueMessage.getProtocolName());
        builder.setRequestIdMSB(uplinkQueueMessage.getMessageIdMSB());
        builder.setRequestIdLSB(uplinkQueueMessage.getMessageIdLSB());
        builder.setRequestData(uplinkQueueMessage.getRequestData());
        return builder;
    }
}