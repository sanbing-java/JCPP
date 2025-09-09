/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.provider;

import sanbing.jcpp.protocol.domain.ProtocolSession;

import java.util.UUID;

/**
 * @author 九筒
 */
public interface ProtocolSessionRegistryProvider {

    /**
     * 注册会话
     */
    void register(ProtocolSession protocolSession);

    void unregister(UUID sessionId);

    ProtocolSession get(UUID sessionId);

    /**
     * 活跃会话
     */
    void activate(ProtocolSession protocolSession);
}