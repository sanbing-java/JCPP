/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.mapping;

import sanbing.jcpp.protocol.domain.DownlinkCmdEnum;

/**
 * 下行命令转换器接口
 * 
 * 每个协议模块都应该实现此接口，提供从通用下行命令到协议特定命令的转换
 *
 * @author sanbing
 * @since 2024-12-16
 */
public interface DownlinkCmdConverter {

    /**
     * 将通用下行命令转换为协议特定的命令字
     *
     * @param downlinkCmd 通用下行命令
     * @return 协议特定的命令字，如果不支持该命令则返回 null
     */
    Integer convertToCmd(DownlinkCmdEnum downlinkCmd);

    /**
     * 检查是否支持指定的下行命令
     *
     * @param downlinkCmd 通用下行命令
     * @return 是否支持
     */
    default boolean supports(DownlinkCmdEnum downlinkCmd) {
        return convertToCmd(downlinkCmd) != null;
    }

    /**
     * 获取协议名称
     *
     * @return 协议名称
     */
    String getProtocolName();
}
