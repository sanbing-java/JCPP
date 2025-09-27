/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.adapter.dto.*;
import sanbing.jcpp.app.dal.config.ibatis.enums.GunRunStatusEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileStatusEnum;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.dal.repository.PileRepository;
import sanbing.jcpp.app.data.kv.*;
import sanbing.jcpp.app.service.*;
import sanbing.jcpp.infrastructure.proto.ProtoConverter;
import sanbing.jcpp.infrastructure.proto.model.PricingModel;
import sanbing.jcpp.infrastructure.proto.model.PricingModel.FlagPrice;
import sanbing.jcpp.infrastructure.proto.model.PricingModel.Period;
import sanbing.jcpp.infrastructure.queue.Callback;
import sanbing.jcpp.infrastructure.util.async.JCPPAsynchron;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.DownlinkProto.*;
import sanbing.jcpp.proto.gen.UplinkProto.*;
import sanbing.jcpp.protocol.domain.DownlinkCmdEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static sanbing.jcpp.proto.gen.DownlinkProto.PricingModelFlag.*;
import static sanbing.jcpp.proto.gen.DownlinkProto.PricingModelRule.PEAK_VALLEY_PRICING;
import static sanbing.jcpp.proto.gen.DownlinkProto.PricingModelType.CHARGE;

/**
 * @author 九筒
 */
@Service
@Slf4j
public class DefaultPileProtocolService implements PileProtocolService {

    @Resource
    PileRepository pileRepository;

    @Resource
    DownlinkCallService downlinkCallService;

    @Resource
    GunService gunService;

    @Resource
    PileService pileService;

    @Resource
    AttributeService attributeService;

    @Resource
    PileSessionService pileSessionService;

    @Override
    public void pileLogin(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.debug("接收到桩登录事件 {}", uplinkQueueMessage);

        LoginRequest loginRequest = uplinkQueueMessage.getLoginRequest();
        String pileCode = loginRequest.getPileCode();

        Pile pile = pileRepository.findPileByCode(pileCode);
        log.debug("查询到充电桩信息: pileCode={}, exists={}", pileCode, pile != null);

        // 构造下行回复
        DownlinkRequestMessage.Builder downlinkMessageBuilder = createDownlinkMessageBuilder(
            uplinkQueueMessage, pileCode);
        downlinkMessageBuilder.setDownlinkCmd(DownlinkCmdEnum.LOGIN_ACK.name());

        if (pile != null) {
            // 处理登录成功的情况
            handleSuccessfulLogin(uplinkQueueMessage, loginRequest, pile, downlinkMessageBuilder, pileCode);
        } else {
            // 处理登录失败的情况（充电桩不存在）
            handleFailedLogin(uplinkQueueMessage, loginRequest, downlinkMessageBuilder, pileCode);
        }

        callback.onSuccess();
    }

    /**
     * 处理登录成功的情况
     */
    private void handleSuccessfulLogin(UplinkQueueMessage uplinkQueueMessage,
                                     LoginRequest loginRequest,
                                     Pile pile,
                                     DownlinkRequestMessage.Builder downlinkMessageBuilder,
                                     String pileCode) {
        try {
            // 1. 保存pileSession
            pileSessionService.createOrUpdateSession(
                    uplinkQueueMessage, pile,
                    loginRequest.getRemoteAddress(),
                    loginRequest.getNodeId(),
                    loginRequest.getNodeHostAddress(),
                    loginRequest.getNodeRestPort(),
                    loginRequest.getNodeGrpcPort());

            // 2. 处理登录状态管理（异步）
            ListenableFuture<AttributesSaveResult> future = pileService.handlePileLogin(pile.getId());

            // 3. 异步处理完成后发送应答
            JCPPAsynchron.withCallback(future,
                result -> {
                    sendLoginSuccessResponse(downlinkMessageBuilder, loginRequest, pileCode);
                    log.info("充电桩登录成功，状态更新完成: pileCode={}", pileCode);
                },
                throwable -> {
                    log.error("充电桩登录状态更新失败: pileCode={}", pileCode, throwable);
                    sendLoginFailureResponse(downlinkMessageBuilder, uplinkQueueMessage, loginRequest, pileCode);
                }
            );

        } catch (Exception e) {
            log.error("处理充电桩登录时发生异常: pileCode={}", pileCode, e);
            sendLoginFailureResponse(downlinkMessageBuilder, uplinkQueueMessage, loginRequest, pileCode);
        }
    }

    /**
     * 处理登录失败的情况
     */
    private void handleFailedLogin(UplinkQueueMessage uplinkQueueMessage,
                                 LoginRequest loginRequest,
                                 DownlinkRequestMessage.Builder downlinkMessageBuilder,
                                 String pileCode) {
        log.warn("充电桩登录失败，充电桩不存在: pileCode={}", pileCode);
        sendLoginFailureResponse(downlinkMessageBuilder, uplinkQueueMessage, loginRequest, pileCode);
    }

    /**
     * 发送登录成功应答
     */
    private void sendLoginSuccessResponse(DownlinkRequestMessage.Builder downlinkMessageBuilder,
                                        LoginRequest loginRequest,
                                        String pileCode) {
        downlinkMessageBuilder.setLoginResponse(LoginResponse.newBuilder()
                .setSuccess(true)
                .setPileCode(loginRequest.getPileCode())
                .build());

        log.info("业务[充电桩登录成功应答] 发送下行消息到充电桩: {}", pileCode);
        downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, pileCode);
    }

    /**
     * 发送登录失败应答
     */
    private void sendLoginFailureResponse(DownlinkRequestMessage.Builder downlinkMessageBuilder,
                                        UplinkQueueMessage uplinkQueueMessage,
                                        LoginRequest loginRequest,
                                        String pileCode) {
        downlinkMessageBuilder.setLoginResponse(LoginResponse.newBuilder()
                .setSuccess(false)
                .setPileCode(loginRequest.getPileCode())
                .build());

        log.info("业务[充电桩登录失败应答] 发送下行消息到充电桩: {}", pileCode);
        downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, uplinkQueueMessage, loginRequest);
    }

    @Override
    public void heartBeat(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.debug("接收到桩心跳事件 {}", uplinkQueueMessage);

        HeartBeatRequest heartBeatRequest = uplinkQueueMessage.getHeartBeatRequest();

        Pile pile = pileRepository.findPileByCode(heartBeatRequest.getPileCode());

        if (pile != null) {
            // 1. 处理心跳状态管理（异步）
            ListenableFuture<AttributesSaveResult> future = pileService.handlePileHeartbeat(pile.getId());

            // 2. 异步处理完成后保存pileSession
            JCPPAsynchron.withCallback(future,
                result -> {
                    // 保存pileSession
                    pileSessionService.createOrUpdateSession(
                            uplinkQueueMessage, pile,
                            heartBeatRequest.getRemoteAddress(),
                            heartBeatRequest.getNodeId(),
                            heartBeatRequest.getNodeHostAddress(),
                            heartBeatRequest.getNodeRestPort(),
                            heartBeatRequest.getNodeGrpcPort());

                    log.debug("充电桩心跳处理完成，状态更新成功: pileCode={}", heartBeatRequest.getPileCode());
                },
                throwable -> {
                    log.error("充电桩心跳状态更新失败: pileCode={}", heartBeatRequest.getPileCode(), throwable);
                }
            );
        }

        callback.onSuccess();
    }

    @Override
    public void onSessionCloseEvent(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到会话关闭事件 {}", uplinkQueueMessage);

        SessionCloseEventProto sessionCloseEvent = uplinkQueueMessage.getSessionCloseEventProto();
        String pileCode = sessionCloseEvent.getPileCode();

        try {
            // 通过 PileService 处理会话关闭状态管理
            pileService.handlePileSessionClose(pileCode);

            // 使用PileSessionService清除会话缓存
            pileSessionService.removeSession(pileCode);

            log.info("会话关闭事件处理完成: 桩编码={}, 关闭原因={}, 状态已更新为OFFLINE",
                    pileCode, sessionCloseEvent.getReason());

        } catch (Exception e) {
            log.error("处理会话关闭事件失败: 桩编码={}", pileCode, e);
        }

        callback.onSuccess();
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

        log.info("业务[计费模型验证应答] 发送下行消息到充电桩: {}, 计费ID: {}", pileCode, pricingId);
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
        model.setRule(PEAK_VALLEY_PRICING);
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

        log.info("业务[计费模型查询应答] 发送下行消息到充电桩: {}", pileCode);
        downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, pileCode);

        callback.onSuccess();
    }

    @Override
    public void postGunRunStatus(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩上报的充电枪状态 {}", uplinkQueueMessage);

        try {
            GunRunStatusProto gunRunStatusProto = uplinkQueueMessage.getGunRunStatusProto();
            String pileCode = gunRunStatusProto.getPileCode();
            String gunNo = gunRunStatusProto.getGunNo();
            long ts = uplinkQueueMessage.getTs();
            GunRunStatus protoStatus = gunRunStatusProto.getGunRunStatus();

            // 委托给 GunService 处理充电枪状态逻辑
            boolean needUpdatePileStatus = gunService.handleGunRunStatus(pileCode, gunNo, protoStatus, ts);

            // 如果需要，根据充电枪状态更新充电桩状态
            if (needUpdatePileStatus) {
                // 转换Proto状态为枚举状态来更新桩状态
                GunRunStatusEnum dbStatus = convertProtoStatusToDbStatus(protoStatus);
                if (dbStatus != null) {
                    updatePileStatusBasedOnGunStatus(pileCode, dbStatus);
                }
            }

        } catch (Exception e) {
            log.error("处理充电枪状态上报失败", e);
            callback.onFailure(e);
            return;
        }

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
        log.info("接收到充电桩上报费率下发反馈 {}", uplinkQueueMessage);

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
    public void onTransactionRecordRequest(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩交易记录上报 {}", uplinkQueueMessage);
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        TransactionRecordRequest transactionRecordRequest = uplinkQueueMessage.getTransactionRecordRequest();

        String tradeNo = transactionRecordRequest.getTradeNo();
        String pileCode = transactionRecordRequest.getPileCode();

        // 构造下行应答
        DownlinkRequestMessage.Builder downlinkMessageBuilder = createDownlinkMessageBuilder(uplinkQueueMessage, pileCode);

        downlinkMessageBuilder.setDownlinkCmd(DownlinkCmdEnum.TRANSACTION_RECORD_ACK.name());
        downlinkMessageBuilder.setTransactionRecordResponse(TransactionRecordResponse.newBuilder()
                .setTradeNo(tradeNo)
                .setSuccess(true)
                .setAdditionalInfo(additionalInfo.toString())
                .build());

        log.info("业务[交易记录上报应答] 发送下行消息到充电桩: {}", pileCode);
        downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder, pileCode);

        callback.onSuccess();
    }

    public void onStartChargeRequest(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩启动充电请求 {}", uplinkQueueMessage);

        StartChargeRequest startChargeRequest = uplinkQueueMessage.getStartChargeRequest();
        String pileCode = startChargeRequest.getPileCode();
        String gunNo = startChargeRequest.getGunNo();
        // TODO 处理相关业务逻辑
        String orderNo = "ORD" + RandomStringUtils.secure().nextNumeric(20);
        String logicalCardNo = RandomStringUtils.secure().nextNumeric(12);

        // 构造下行回复
        DownlinkRequestMessage.Builder downlinkMessageBuilder = createDownlinkMessageBuilder(uplinkQueueMessage, startChargeRequest.getPileCode());

        downlinkMessageBuilder.setDownlinkCmd(DownlinkCmdEnum.START_CHARGE_ACK.name());
        downlinkMessageBuilder.setStartChargeResponse(StartChargeResponse.newBuilder()
                .setTradeNo(orderNo)
                .setPileCode(startChargeRequest.getPileCode())
                .setGunNo(startChargeRequest.getGunNo())
                .setLogicalCardNo(logicalCardNo)
                .setLimitYuan("50")
                .setAuthSuccess(true)
                .setFailReason(FailReason.ACCOUNT_NOT_ALLOWED_ON_PILE.name())
        );

        log.info("业务[充电桩启动充电请求应答] 发送下行消息到充电桩: {}, 充电枪: {}", pileCode, gunNo);
        downlinkCallService.sendDownlinkMessage(downlinkMessageBuilder,pileCode);

        callback.onSuccess();
    }

    @Override
    public void startCharge(StartChargeDTO startChargeDto) {
        String pileCode = startChargeDto.getPileCode();
        String gunNo = startChargeDto.getGunNo();
        BigDecimal limitYuan = startChargeDto.getLimitYuan();
        String orderNo = startChargeDto.getOrderNo();
        String logicalCardNo = startChargeDto.getLogicalCardNo();
        String physicalCardNo = startChargeDto.getPhysicalCardNo();
        String parallelNo = startChargeDto.getParallelNo();

        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        RemoteStartChargingRequest.Builder requestBuilder = RemoteStartChargingRequest.newBuilder()
                .setPileCode(pileCode)
                .setGunNo(gunNo)
                .setLimitYuan(limitYuan.toPlainString())
                .setTradeNo(orderNo);

        // 添加可选字段
        if (logicalCardNo != null) {
            requestBuilder.setLogicalCardNo(logicalCardNo);
        }
        if (physicalCardNo != null) {
            requestBuilder.setPhysicalCardNo(physicalCardNo);
        }
        if (parallelNo != null) {
            requestBuilder.setParallelNo(parallelNo);
        }

        // 根据是否有并充序号自动选择命令类型
        DownlinkCmdEnum downlinkCmd = (parallelNo != null && !parallelNo.trim().isEmpty()) 
                ? DownlinkCmdEnum.REMOTE_PARALLEL_START_CHARGING 
                : DownlinkCmdEnum.REMOTE_START_CHARGING;

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(pileCode)
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(downlinkCmd.name())
                .setRemoteStartChargingRequest(requestBuilder.build());

        log.info("业务[远程启动充电] 发送下行消息到充电桩: {}, 充电枪: {}, 订单号: {}", pileCode, gunNo, orderNo);
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder, pileCode);
    }

    @Override
    public void stopCharge(StopChargeDTO stopChargeDto) {
        String pileCode = stopChargeDto.getPileCode();
        String gunNo = stopChargeDto.getGunNo();

        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(pileCode)
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.REMOTE_STOP_CHARGING.name())
                .setRemoteStopChargingRequest(RemoteStopChargingRequest.newBuilder()
                        .setPileCode(pileCode)
                        .setGunNo(gunNo)
                        .build());

        log.info("业务[远程停止充电] 发送下行消息到充电桩: {}, 充电枪: {}", pileCode, gunNo);
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder, pileCode);
    }

    @Override
    public void restartPile(RestartPileDTO restartPileDto) {
        String pileCode = restartPileDto.getPileCode();
        Integer type = restartPileDto.getType();

        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(pileCode)
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.REMOTE_RESTART_PILE.name())
                .setRestartPileRequest(RestartPileRequest.newBuilder()
                        .setPileCode(pileCode)
                        .setType(type)
                        .build());

        log.info("业务[重启充电桩] 发送下行消息到充电桩: {}, 重启类型: {}", pileCode, type);
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder, pileCode);
    }

    @Override
    public void setPricing(SetPricingDTO setPricingDto) {
        String pileCode = setPricingDto.getPileCode();
        SetPricingRequest setPricingRequest = setPricingDto.getSetPricingRequest();
        
        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(pileCode)
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.SET_PRICING.name())
                .setSetPricingRequest(setPricingRequest);

        log.info("业务[设置计费模型] 发送下行消息到充电桩: {}", pileCode);
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder, pileCode);
    }

    @Override

    public void onBmsChargingErrorProto(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("充电桩与 BMS 充电错误上报 {}", uplinkQueueMessage);
    }

    public void postBmsAbort(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电阶段BMS中止报文 {}", uplinkQueueMessage);
        // TODO 处理相关业务逻辑
        callback.onSuccess();
    }

    @Override
    public void onBmsParamConfigReport(UplinkQueueMessage uplinkQueueMsg, Callback callback) {
        log.info("充电桩与 BMS 参数配置阶段报文 {}", uplinkQueueMsg);

        callback.onSuccess();
    }


    @Override
    public void onBmsCharingInfo(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩上报BMS充电信息 {}", uplinkQueueMessage);
        BmsChargingInfoProto bmsCharingInfoProto = uplinkQueueMessage.getBmsChargingInfoProto();
        String tradeNo = bmsCharingInfoProto.getTradeNo();
        String pileCode = bmsCharingInfoProto.getPileCode();
        String gunNo = bmsCharingInfoProto.getGunNo();
        String additionalInfo = bmsCharingInfoProto.getAdditionalInfo();
        log.info("BMS充电信息: 交易流水号: {}, 桩编码: {}, 枪号: {}, 附加信息: {}", tradeNo, pileCode, gunNo, additionalInfo);
        // TODO 处理相关业务逻辑
        callback.onSuccess();
    }

    @Override
    public void onRestartPileResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩重启结果反馈 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void otaRequest(OtaRequest request) {

        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(request.getPileCode())
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.OTA_REQUEST.name())
                .setOtaRequest(request);
        log.info("业务[OTA升级请求] 发送下行消息到充电桩: {}, 文件路径: {}", request.getPileCode(), request.getFilePath());
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder,request.getPileCode());

    }

    @Override
    public void onOtaResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {

        log.info("接收到充电桩更新应答 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();

    }

    @Override
    public void postBmsHandshake(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到BMS充电握手信息 {}", uplinkQueueMessage);
        BmsHandshakeProto bmsHandshakeProto = uplinkQueueMessage.getBmsHandshakeProto();
        String tradeNo = bmsHandshakeProto.getTradeNo();
        String pileCode = bmsHandshakeProto.getPileCode();
        String gunNo = bmsHandshakeProto.getGunNo();
        String carVinCode = bmsHandshakeProto.getCarVinCode();
        String bmsProtocolVersion = bmsHandshakeProto.getBmsProtocolVersion();
        int bmsBatteryType = bmsHandshakeProto.getBmsBatteryType();
        int bmsPowerCapacity = bmsHandshakeProto.getBmsPowerCapacity();
        String additionalInfo = bmsHandshakeProto.getAdditionalInfo();
        
        log.info("BMS充电握手信息: 交易流水号: {}, 桩编码: {}, 枪号: {}, 车辆VIN: {}, BMS协议版本: {}, " +
                        "电池类型: {}, 电池容量: {}Ah, 附加信息: {}",
                tradeNo, pileCode, gunNo, carVinCode, bmsProtocolVersion, 
                bmsBatteryType, bmsPowerCapacity, additionalInfo);
        
        // TODO 处理相关业务逻辑，比如保存握手信息到数据库

        callback.onSuccess();
    }

    @Override
    public void timeSync(TimeSyncDTO timeSyncDto) {
        String pileCode = timeSyncDto.getPileCode();
        LocalDateTime time = timeSyncDto.getTime();
        
        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(pileCode)
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.SYNC_TIME_REQUEST.name())
                .setTimeSyncRequest(TimeSyncRequest.newBuilder()
                        .setPileCode(pileCode)
                        .setTime(DateUtil.formatLocalDateTime(time))
                        .build());
        log.info("业务[时间同步] 发送下行消息到充电桩: {}, 同步时间: {}", pileCode, DateUtil.formatLocalDateTime(time));
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder, pileCode);
    }

    @Override
    public void onTimeSyncResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("对时设置应答 {}", uplinkQueueMessage);
        TimeSyncResponse timeSyncResponse = uplinkQueueMessage.getTimeSyncResponse();
        String pileCode = timeSyncResponse.getPileCode();
        String time = timeSyncResponse.getTime();
        log.info("对时设置应答: 桩编码: {}, 时间: {}", pileCode, time);
        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void setQrcode(SetQrcodeRequest setQrcodeRequest) {
        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String pileCode = setQrcodeRequest.getPileCode();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(pileCode)
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.SET_QRCODE.name())
                .setSetQrcodeRequest(setQrcodeRequest);

        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder, pileCode);
    }

    @Override
    public void onSetQrcodeResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("下发充电桩字符型应答 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void postLockStatus(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到地锁状态信息 {}", uplinkQueueMessage);
        GroundLockStatusProto groundLockStatusProto = uplinkQueueMessage.getGroundLockStatusProto();
        String pileCode = groundLockStatusProto.getPileCode();
        String gunNo = groundLockStatusProto.getGunNo();
        int lockStatus = groundLockStatusProto.getLockStatus();
        int parkStatus = groundLockStatusProto.getParkStatus();
        int lockBattery = groundLockStatusProto.getLockBattery();
        int alarmStatus = groundLockStatusProto.getAlarmStatus();

        log.info("地锁状态信息: 桩编码: {}, 枪号: {}, 车位锁状态: {}, 车位状态: {}, 地锁电量: {}%, 报警状态: {}",
                pileCode, gunNo, lockStatus, parkStatus, lockBattery, alarmStatus);

        try {
            // 获取时间戳
            long ts = uplinkQueueMessage.getTs();

            // 获取充电枪信息
            // 注意：充电桩上报的gunNo实际上是枪编号(gun_no)，不是完整的枪编码(gun_code)
            Gun gun = gunService.findByPileCodeAndGunNo(pileCode, gunNo);
            if (gun != null) {
                // 保存地锁状态到属性表
                saveLockStatusToAttributes(gun.getId(), lockStatus, parkStatus, lockBattery, alarmStatus, ts);

                log.info("地锁和车位状态已保存: 桩编码={}, 枪编码={}, 地锁状态={}, 车位状态={}",
                        pileCode, gunNo, lockStatus, parkStatus);
            } else {
                log.warn("未找到充电枪，无法保存地锁状态: 桩编码={}, 枪编码={}", pileCode, gunNo);
            }
        } catch (Exception e) {
            log.error("保存地锁状态失败: 桩编码={}, 枪编码={}", pileCode, gunNo, e);
            callback.onFailure(e);
            return;
        }

        callback.onSuccess();
    }


    @Override
    public void onOfflineCardBalanceUpdateResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩远程账户余额更新应答 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void offlineCardBalanceUpdateRequest(OfflineCardBalanceUpdateRequest request) {
        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(request.getPileCode())
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.OFFLINE_CARD_BALANCE_UPDATE_REQUEST.name())
                .setOfflineCardBalanceUpdateRequest(request);
        log.info("业务[离线卡余额更新] 发送下行消息到充电桩: {}, 卡号: {}", request.getPileCode(), request.getCardNo());
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder,request.getPileCode());
    }

    @Override
    public void offlineCardSyncRequest(OfflineCardSyncRequest request) {
        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(request.getPileCode())
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.OFFLINE_CARD_SYNC_REQUEST.name())
                .setOfflineCardSyncRequest(request);
        log.info("业务[离线卡同步] 发送下行消息到充电桩: {}", request.getPileCode());
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder,request.getPileCode());
    }

    @Override
    public void onOfflineCardSyncResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩离线卡数据同步应答 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void offlineCardClearRequest(OfflineCardClearRequest request) {
        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(request.getPileCode())
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.OFFLINE_CARD_CLEAR_REQUEST.name())
                .setOfflineCardClearRequest(request);
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder,request.getPileCode());
    }

    @Override
    public void onOfflineCardClearResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩离线卡数据清除应答 {}", uplinkQueueMessage);

        // TODO 处理相关业务逻辑

        callback.onSuccess();
    }

    @Override
    public void offlineCardQueryRequest(OfflineCardQueryRequest request) {
        UUID messageId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        DownlinkRequestMessage.Builder downlinkRequestMessageBuilder = DownlinkRequestMessage.newBuilder()
                .setMessageIdMSB(messageId.getMostSignificantBits())
                .setMessageIdLSB(messageId.getLeastSignificantBits())
                .setPileCode(request.getPileCode())
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setDownlinkCmd(DownlinkCmdEnum.OFFLINE_CARD_QUERY_REQUEST.name())
                .setOfflineCardQueryRequest(request);
        downlinkCallService.sendDownlinkMessage(downlinkRequestMessageBuilder,request.getPileCode());
    }

    @Override
    public void onOfflineCardQueryResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电桩离线卡数据查询应答 {}", uplinkQueueMessage);


        // TODO 处理相关业务逻辑

        callback.onSuccess();
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

    @Override
    public void postBmsDemandChargerOutput(UplinkQueueMessage uplinkQueueMessage, Callback callback) {
        log.info("接收到充电过程BMS需求与充电机输出信息:{}", uplinkQueueMessage);
        BmsDemandChargerOutputProto bmsDemandChargerOutputProto = uplinkQueueMessage.getBmsDemandChargerOutputProto();
        String pileCode = bmsDemandChargerOutputProto.getPileCode();
        String gunNo = bmsDemandChargerOutputProto.getGunNo();
        String tradeNo = bmsDemandChargerOutputProto.getTradeNo();
        String additionalInfo = bmsDemandChargerOutputProto.getAdditionalInfo();
        log.info("充电过程BMS需求与充电机输出信息: 桩编码: {}, 枪号: {}, 交易流水号: {}, 附加信息: {}",
                pileCode, gunNo, tradeNo, additionalInfo);
        // TODO 处理相关业务逻辑
        callback.onSuccess();
    }

    /**
     * 将Proto状态转换为数据库枚举状态
     */
    private GunRunStatusEnum convertProtoStatusToDbStatus(GunRunStatus protoStatus) {
        switch (protoStatus) {
            case IDLE:
                return GunRunStatusEnum.IDLE;
            case INSERTED:
                return GunRunStatusEnum.INSERTED;
            case CHARGING:
                return GunRunStatusEnum.CHARGING;
            case CHARGE_COMPLETE:
                return GunRunStatusEnum.CHARGE_COMPLETE;
            case DISCHARGE_READY:
                return GunRunStatusEnum.DISCHARGE_READY;
            case DISCHARGING:
                return GunRunStatusEnum.DISCHARGING;
            case DISCHARGE_COMPLETE:
                return GunRunStatusEnum.DISCHARGE_COMPLETE;
            case RESERVED:
                return GunRunStatusEnum.RESERVED;
            case FAULT:
                return GunRunStatusEnum.FAULT;
            case UNKNOWN:
            default:
                return null; // 未知状态不更新
        }
    }

    /**
     * 充电枪状态上报处理
     * <p>
     * 重构说明：
     * - 充电桩状态简化为ONLINE/OFFLINE，不再受枪状态影响
     * - 充电枪的工作状态独立维护，不影响充电桩的在线状态
     * - 只要设备能正常通信，充电桩就保持ONLINE状态
     * <p>
     * 处理逻辑：
     * 1. 枪状态变化不影响桩的在线状态
     * 2. 仅在设备能上报枪状态时，确保桩为ONLINE状态
     * 3. 枪的故障状态仅记录在枪级别，不影响桩状态
     */
    private void updatePileStatusBasedOnGunStatus(String pileCode, GunRunStatusEnum gunStatus) {
        // 枪状态上报说明设备在线，确保充电桩状态为ONLINE
        Pile pile = pileRepository.findPileByCode(pileCode);
        if (pile != null) {
            String currentStatusStr = pileService.findPileStatus(pile.getId());

            // 如果当前不是ONLINE状态，更新为ONLINE
            if (!"ONLINE".equals(currentStatusStr)) {
                pileService.updatePileStatusByCode(pileCode, PileStatusEnum.ONLINE);
                log.info("枪状态上报，确保充电桩在线: 桩编码={}, 枪状态={}, 更新桩状态=ONLINE",
                        pileCode, gunStatus);
            }
        }

        // 注意：枪的具体状态通过GunService单独管理，与桩状态解耦
        log.debug("充电枪状态上报: 桩编码={}, 枪状态={}", pileCode, gunStatus);
    }

    /**
     * 保存地锁状态到属性表
     */
    private void saveLockStatusToAttributes(UUID gunId, int lockStatus, int parkStatus, int lockBattery, int alarmStatus, long ts) {
        try {

            // 保存地锁状态
            AttributeKvEntry lockStatusAttr = new BaseAttributeKvEntry(
                new LongDataEntry(AttrKeyEnum.LOCK_STATUS.getCode(), (long) lockStatus),
                ts
            );
            attributeService.save(gunId, lockStatusAttr);

            // 保存车位状态
            AttributeKvEntry parkStatusAttr = new BaseAttributeKvEntry(
                new LongDataEntry(AttrKeyEnum.PARK_STATUS.getCode(), (long) parkStatus),
                ts
            );
            attributeService.save(gunId, parkStatusAttr);

            // 地锁电量和报警状态暂不保存，只记录日志
            log.debug("地锁电量: {}%, 报警状态: {}", lockBattery, alarmStatus);

        } catch (Exception e) {
            log.error("保存地锁状态到属性表失败: gunId={}", gunId, e);
            throw e;
        }
    }

}