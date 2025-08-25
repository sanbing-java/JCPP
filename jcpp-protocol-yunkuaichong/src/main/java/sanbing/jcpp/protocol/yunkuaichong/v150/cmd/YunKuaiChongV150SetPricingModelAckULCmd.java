/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.SetPricingResponse;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;
import static sanbing.jcpp.protocol.yunkuaichong.enums.YunKuaiChongDownlinkCmdEnum.SET_PRICING;

/**
 * 云快充1.5.0 计费模型应答
 *
 * @author baigod
 */
@Slf4j
@YunKuaiChongCmd(value = 0x57, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150SetPricingModelAckULCmd extends YunKuaiChongUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0计费模型应答", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        // 1.桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 2.设置结果 0x00:失败 0x01:成功
        boolean isSuccess = (byteBuf.readByte() == 0x01);

        // 从缓存取上个请求的pricingId
        Object pricingId = tcpSession.getRequestCache().asMap().getOrDefault(pileCode + SET_PRICING.getCmd(), null);

        if (pricingId instanceof Long pricingIdL) {
            // 转发到后端
            SetPricingResponse setPricingResponse = SetPricingResponse.newBuilder()
                    .setPileCode(pileCode)
                    .setSuccess(isSuccess)
                    .setPricingId(pricingIdL)
                    .build();
            UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(setPricingResponse.getPileCode(), tcpSession, yunKuaiChongUplinkMessage)
                    .setSetPricingResponse(setPricingResponse)
                    .build();
            tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
        }


    }
}