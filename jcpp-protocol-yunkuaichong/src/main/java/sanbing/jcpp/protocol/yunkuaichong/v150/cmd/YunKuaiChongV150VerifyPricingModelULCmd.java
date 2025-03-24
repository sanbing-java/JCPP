/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;


import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.proto.gen.ProtocolProto.VerifyPricingRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;


/**
 * 云快充1.5.0计费模型验证请求
 *
 * @author baigod
 */
@Slf4j
@YunKuaiChongCmd(0x05)
public class YunKuaiChongV150VerifyPricingModelULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0计费模型验证请求", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        byte[] pricingModelIdBytes = new byte[2];
        byteBuf.readBytes(pricingModelIdBytes);
        long pricingModelId = BCDUtil.bcdBytesToLong(pricingModelIdBytes);

        // 转发到后端
        VerifyPricingRequest heartBeatRequest = VerifyPricingRequest.newBuilder()
                .setPileCode(pileCode)
                .setPricingId(pricingModelId)
                .setAdditionalInfo(additionalInfo.toString())
                .build();
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(heartBeatRequest.getPileCode(), tcpSession, yunKuaiChongUplinkMessage)
                .setVerifyPricingRequest(heartBeatRequest)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);

    }
}