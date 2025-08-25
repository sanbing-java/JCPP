/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import sanbing.jcpp.infrastructure.queue.Callback;
import sanbing.jcpp.proto.gen.ProtocolProto;
import sanbing.jcpp.proto.gen.ProtocolProto.OfflineCardBalanceUpdateRequest;
import sanbing.jcpp.proto.gen.ProtocolProto.OfflineCardSyncRequest;
import sanbing.jcpp.proto.gen.ProtocolProto.SetPricingRequest;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author baigod
 */
public interface PileProtocolService {
    /**
     * 桩登录
     */
    void pileLogin(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 充电桩心跳
     */
    void heartBeat(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 校验计费模型
     */
    void verifyPricing(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 查询计费策略
     */
    void queryPricing(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 上报电桩运行状态
     */
    void postGunRunStatus(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 上报充电进度
     */
    void postChargingProgress(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 费率下发反馈
     */
    void onSetPricingResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 远程启动反馈
     *
     * @param uplinkQueueMessage
     * @param callback
     */
    void onRemoteStartChargingResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 远程停止反馈
     */
    void onRemoteStopChargingResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 交易记录上报
     */
    void onTransactionRecordRequest(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 启动充电（支持卡号和并充序号）
     * 当 parallelNo 不为空时，自动使用并充启机命令
     */
    void startCharge(String pileCode, String gunCode, BigDecimal limitYuan, String orderNo, 
                    String logicalCardNo, String physicalCardNo, String parallelNo);

    /**
     * 停止充电
     */
    void stopCharge(String pileCode, String gunCode);

    /**
     * 重启充电
     */
    void restartPile(String pileCode, Integer type);

    /**
     * 下发计费
     */
    void setPricing(String pileCode, SetPricingRequest setPricingRequest);

    /**
     * 充电桩与 BMS 充电错误上报
     */
    void onBmsChargingErrorProto(UplinkQueueMessage uplinkQueueMsg, Callback callback);

    /**
     * 充电桩与 BMS 参数配置阶段报文
     */
    void onBmsParamConfigReport(UplinkQueueMessage uplinkQueueMsg, Callback callback);

    /**
     * 充电过程BMS信息
     */
    void onBmsCharingInfo(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 远程重启反馈
     */
    void onRestartPileResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 充电阶段BMS中止
     */
    void postBmsAbort(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     *  远程更新
     */
    void otaRequest(ProtocolProto.OtaRequest request);

    /**
     * 远程更新应答
     */
    void onOtaResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 处理BMS握手信息
     *
     * @param uplinkQueueMessage 上行消息
     * @param callback           回调
     */
    void postBmsHandshake(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 处理地锁状态信息
     *
     * @param uplinkQueueMessage 上行消息
     * @param callback           回调
     */
    void postLockStatus(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 远程账户余额更新
     */
    void offlineCardBalanceUpdateRequest(OfflineCardBalanceUpdateRequest request);

    /**
     * 远程账户余额更新应答
     */
    void onOfflineCardBalanceUpdateResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 离线卡数据同步
     */
    void offlineCardSyncRequest(OfflineCardSyncRequest request);

    /**
     * 离线卡数据同步应答
     */
    void onOfflineCardSyncResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 实时同步桩时间
     */
    void timeSync(String pileCode, LocalDateTime time);

    /**
     * 实时同步桩时间应答
     */
    void onTimeSyncResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

}