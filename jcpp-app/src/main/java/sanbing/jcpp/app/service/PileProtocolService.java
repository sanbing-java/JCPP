/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import sanbing.jcpp.app.adapter.dto.*;
import sanbing.jcpp.infrastructure.queue.Callback;
import sanbing.jcpp.proto.gen.DownlinkProto;
import sanbing.jcpp.proto.gen.DownlinkProto.OfflineCardBalanceUpdateRequest;
import sanbing.jcpp.proto.gen.DownlinkProto.OfflineCardSyncRequest;
import sanbing.jcpp.proto.gen.DownlinkProto.OtaRequest;
import sanbing.jcpp.proto.gen.DownlinkProto.SetQrcodeRequest;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;

/**
 * @author 九筒
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
     * 处理会话关闭事件
     */
    void onSessionCloseEvent(UplinkQueueMessage uplinkQueueMessage, Callback callback);

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
     * 充电桩主动申请启动充电
     */
    void onStartChargeRequest(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 启动充电（支持卡号和并充序号）
     * 当 parallelNo 不为空时，自动使用并充启机命令
     */
    void startCharge(StartChargeDTO startChargeDto);

    /**
     * 停止充电
     */
    void stopCharge(StopChargeDTO stopChargeDto);

    /**
     * 重启充电桩
     */
    void restartPile(RestartPileDTO restartPileDto);

    /**
     * 下发计费策略
     */
    void setPricing(SetPricingDTO setPricingDto);

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
     * 远程更新
     */
    void otaRequest(OtaRequest request);

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
    void timeSync(TimeSyncDTO timeSyncDto);

    /**
     * 实时同步桩时间应答
     */
    void onTimeSyncResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 离线卡数据清除
     */
    void offlineCardClearRequest(DownlinkProto.OfflineCardClearRequest request);

    /**
     * 离线卡数据清除应答
     */
    void onOfflineCardClearResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 离线卡数据查询
     */
    void offlineCardQueryRequest(DownlinkProto.OfflineCardQueryRequest request);

    /**
     * 离线卡数据查询应答
     */
    void onOfflineCardQueryResponse(UplinkQueueMessage uplinkQueueMessage, Callback callback);
    /**
     * 充电过程BMS需求与充电机输出
     */
    void postBmsDemandChargerOutput(UplinkQueueMessage uplinkQueueMessage, Callback callback);

    /**
     * 服务器下发充电桩字符型参数
     */
    void setQrcode( SetQrcodeRequest setQrcodeRequest );
    /**
     * 服务器下发充电桩字符型参数反馈
     */
    void onSetQrcodeResponse(UplinkQueueMessage uplinkQueueMsg, Callback callback);

    /**
     * 充电桩工作参数设置
     */
    void workParamSettingRequest(DownlinkProto.WorkParamSettingRequest request);

    /**
     * 充电桩工作参数设置应答
     */
    void onWorkParamSettingRequest(UplinkQueueMessage uplinkQueueMsg, Callback callback);

    /**
     * 结束充电
     */
    void onEndCharge(UplinkQueueMessage uplinkQueueMsg, Callback callback);
}