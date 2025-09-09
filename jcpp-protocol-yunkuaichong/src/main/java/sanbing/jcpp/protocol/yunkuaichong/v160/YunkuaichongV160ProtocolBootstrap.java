/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v160;

import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.annotation.ProtocolComponent;
import sanbing.jcpp.protocol.ProtocolBootstrap;
import sanbing.jcpp.protocol.ProtocolMessageProcessor;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolMessageProcessor;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.YUNKUAICHONG_V160;

/**
 * @author 九筒
 */

@ProtocolComponent(YUNKUAICHONG_V160)
@Slf4j
public class YunkuaichongV160ProtocolBootstrap extends ProtocolBootstrap {

    public static final String PROTOCOL_NAME = YUNKUAICHONG_V160;

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
        return new YunKuaiChongProtocolMessageProcessor(forwarder, protocolContext);
    }


}