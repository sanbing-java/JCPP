/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.v340;

import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.annotation.ProtocolComponent;
import sanbing.jcpp.protocol.ProtocolBootstrap;
import sanbing.jcpp.protocol.ProtocolMessageProcessor;
import sanbing.jcpp.protocol.lvneng.LvnengProtocolMessageProcessor;

import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.LVNENG_V340;

@ProtocolComponent(LVNENG_V340)
@Slf4j
public class LvnengV340ProtocolBootstrap extends ProtocolBootstrap {

    public static final String PROTOCOL_NAME = LVNENG_V340;
    @Override
    protected String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected void _init() {
        // do nothing
    }

    @Override
    protected void _destroy() {
        // do nothing
    }

    @Override
    protected ProtocolMessageProcessor messageProcessor() {
        return new LvnengProtocolMessageProcessor(forwarder, protocolContext);
    }
}
