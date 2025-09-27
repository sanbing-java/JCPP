/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sanbing.jcpp.app.adapter.dto.*;
import sanbing.jcpp.app.adapter.request.RpcRequest;
import sanbing.jcpp.app.adapter.response.ApiResponse;
import sanbing.jcpp.app.service.PileProtocolService;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.DownlinkProto.*;

/**
 * RPC控制器 - 通用化的充电桩下行指令接口
 *
 * @author 九筒
 */
@RestController
@RequestMapping("/api/rpc")
@RequiredArgsConstructor
@Slf4j
public class RpcController extends BaseController {

    private final PileProtocolService pileProtocolService;

    /**
     * 单向RPC接口 - 不等待充电桩返回消息
     */
    @PostMapping("/oneway")
    public ResponseEntity<ApiResponse<String>> onewayRpc(@RequestBody RpcRequest request) {
        try {
            log.info("收到单向RPC请求: method={}, parameter={}", request.getMethod(), request.getParameter());

            // 根据method调用对应的服务方法
            executeRpcMethod(request.getMethod(), request.getParameter());

            return ResponseEntity.ok(ApiResponse.success("指令发送成功", null));
        } catch (Exception e) {
            log.error("单向RPC调用失败: method={}, error={}", request.getMethod(), e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("指令发送失败: " + e.getMessage(), null));
        }
    }

    /**
     * 双向RPC接口 - 等待充电桩返回消息（带超时）
     * TODO: 实现双向RPC逻辑，包括超时处理
     */
    @PostMapping("/bidirectional")
    public ResponseEntity<ApiResponse<String>> bidirectionalRpc(@RequestBody RpcRequest request) {
        // TODO: 实现双向RPC，需要等待充电桩响应，包含超时时间参数
        return ResponseEntity.ok(ApiResponse.error("双向RPC功能待实现", null));
    }

    /**
     * 执行RPC方法
     */
    private void executeRpcMethod(String method, JsonNode parameter) throws Exception {
        switch (method) {
            case "startCharge":
                handleStartCharge(parameter);
                break;
            case "stopCharge":
                handleStopCharge(parameter);
                break;
            case "restartPile":
                handleRestartPile(parameter);
                break;
            case "setPricing":
                handleSetPricing(parameter);
                break;
            case "setQrcode":
                handleSetQrcode(parameter);
                break;
            case "otaRequest":
                handleOtaRequest(parameter);
                break;
            case "offlineCardBalanceUpdate":
                handleOfflineCardBalanceUpdate(parameter);
                break;
            case "offlineCardSync":
                handleOfflineCardSync(parameter);
                break;
            case "offlineCardClear":
                handleOfflineCardClear(parameter);
                break;
            case "offlineCardQuery":
                handleOfflineCardQuery(parameter);
                break;
            case "timeSync":
                handleTimeSync(parameter);
                break;
            default:
                throw new IllegalArgumentException("不支持的RPC方法: " + method);
        }
    }

    /**
     * 处理启动充电指令
     */
    private void handleStartCharge(JsonNode parameter) {
        StartChargeDTO startChargeDto = JacksonUtil.fromJson(parameter, StartChargeDTO.class);
        pileProtocolService.startCharge(startChargeDto);
    }

    /**
     * 处理停止充电指令
     */
    private void handleStopCharge(JsonNode parameter) {
        StopChargeDTO stopChargeDto = JacksonUtil.fromJson(parameter, StopChargeDTO.class);
        pileProtocolService.stopCharge(stopChargeDto);
    }

    /**
     * 处理重启充电桩指令
     */
    private void handleRestartPile(JsonNode parameter) {
        RestartPileDTO restartPileDto = JacksonUtil.fromJson(parameter, RestartPileDTO.class);
        pileProtocolService.restartPile(restartPileDto);
    }

    /**
     * 处理设置计费策略指令
     */
    private void handleSetPricing(JsonNode parameter) {
        SetPricingDTO setPricingDto = JacksonUtil.fromJson(parameter, SetPricingDTO.class);
        pileProtocolService.setPricing(setPricingDto);
    }

    /**
     * 处理设置二维码指令
     */
    private void handleSetQrcode(JsonNode parameter) {
        SetQrcodeRequest setQrcodeRequest = JacksonUtil.fromJson(parameter, SetQrcodeRequest.class);
        pileProtocolService.setQrcode(setQrcodeRequest);
    }

    /**
     * 处理OTA升级指令
     */
    private void handleOtaRequest(JsonNode parameter) {
        OtaRequest otaRequest = JacksonUtil.fromJson(parameter, OtaRequest.class);
        pileProtocolService.otaRequest(otaRequest);
    }

    /**
     * 处理离线卡余额更新指令
     */
    private void handleOfflineCardBalanceUpdate(JsonNode parameter) {
        OfflineCardBalanceUpdateRequest request = JacksonUtil.fromJson(parameter, OfflineCardBalanceUpdateRequest.class);
        pileProtocolService.offlineCardBalanceUpdateRequest(request);
    }

    /**
     * 处理离线卡同步指令
     */
    private void handleOfflineCardSync(JsonNode parameter) {
        OfflineCardSyncRequest request = JacksonUtil.fromJson(parameter, OfflineCardSyncRequest.class);
        pileProtocolService.offlineCardSyncRequest(request);
    }

    /**
     * 处理离线卡清除指令
     */
    private void handleOfflineCardClear(JsonNode parameter) {
        OfflineCardClearRequest request = JacksonUtil.fromJson(parameter, OfflineCardClearRequest.class);
        pileProtocolService.offlineCardClearRequest(request);
    }

    /**
     * 处理离线卡查询指令
     */
    private void handleOfflineCardQuery(JsonNode parameter) {
        OfflineCardQueryRequest request = JacksonUtil.fromJson(parameter, OfflineCardQueryRequest.class);
        pileProtocolService.offlineCardQueryRequest(request);
    }

    /**
     * 处理时间同步指令
     */
    private void handleTimeSync(JsonNode parameter) {
        TimeSyncDTO timeSyncDto = JacksonUtil.fromJson(parameter, TimeSyncDTO.class);
        pileProtocolService.timeSync(timeSyncDto);
    }

}
