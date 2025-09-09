/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sanbing.jcpp.protocol.enums.SupportedProtocols;

/**
 * 协议选项响应
 * 用于前端下拉选择组件
 * 
 * @author 九筒
 * @since 2024-12-22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProtocolOption {
    
    private String value;       // 协议标识符（用于表单提交）
    private String label;       // 显示名称（用于前端显示）
    
    /**
     * 从协议信息创建选项
     * @param protocolInfo 协议信息
     * @return 协议选项
     */
    public static ProtocolOption fromProtocolInfo(SupportedProtocols.ProtocolInfo protocolInfo) {
        return new ProtocolOption(
            protocolInfo.protocolId(),
            protocolInfo.displayName()
        );
    }
}
