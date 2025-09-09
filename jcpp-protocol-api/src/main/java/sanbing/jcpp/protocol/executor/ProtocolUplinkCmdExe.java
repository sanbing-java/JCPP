/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.executor;

import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.domain.ProtocolSession;

/**
 * 通用协议上行命令执行器接口
 *
 * @param <T> 上行消息类型
 * @author 九筒
 * @since 2024-12-16
 */
public interface ProtocolUplinkCmdExe<T> {

    /**
     * 执行上行命令
     *
     * @param session         TCP会话
     * @param uplinkMessage   上行消息
     * @param protocolContext 协议上下文
     */
    void execute(ProtocolSession session, T uplinkMessage, ProtocolContext protocolContext);
}
