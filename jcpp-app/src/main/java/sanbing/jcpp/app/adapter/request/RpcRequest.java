/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RPC请求参数
 *
 * @author 九筒
 */
@Data
public class RpcRequest {

    /**
     * RPC方法名
     * 支持的方法包括：
     * - startCharge: 启动充电
     * - stopCharge: 停止充电
     * - restartPile: 重启充电桩
     * - setPricing: 设置计费策略
     * - setQrcode: 设置二维码
     * - otaRequest: OTA升级
     * - offlineCardBalanceUpdate: 离线卡余额更新
     * - offlineCardSync: 离线卡同步
     * - offlineCardClear: 离线卡清除
     * - offlineCardQuery: 离线卡查询
     * - timeSync: 时间同步
     */
    @NotBlank(message = "方法名不能为空")
    private String method;

    /**
     * 方法参数，JSON格式
     * 不同的方法需要不同的参数结构
     */
    @NotNull(message = "参数不能为空")
    private JsonNode parameter;

    /**
     * 超时时间（毫秒），仅用于双向RPC
     */
    private Long timeoutMs;
}
