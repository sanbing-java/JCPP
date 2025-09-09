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
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.VerifyPricingResponse;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.util.Arrays;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.VERIFY_PRICING_ACK;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage.FAILURE_BYTE;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage.SUCCESS_BYTE;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0计费模型验证请求应答
 *
 * @author 九筒
 */
@Slf4j
@ProtocolCmd(value = 0x06, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150VerifyPricingModelAckDLCmd extends YunKuaiChongDownlinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0计费模型验证请求应答", tcpSession);

        if (!yunKuaiChongDwonlinkMessage.getMsg().hasVerifyPricingResponse()) {
            return;
        }

        VerifyPricingResponse verifyPricingResponse = yunKuaiChongDwonlinkMessage.getMsg().getVerifyPricingResponse();

        YunKuaiChongUplinkMessage requestData = JacksonUtil.fromBytes(yunKuaiChongDwonlinkMessage.getMsg().getRequestData().toByteArray(), YunKuaiChongUplinkMessage.class);

        // 获取上行报文
        byte[] uplinkRawFrame = requestData.getRawFrame();
        // 从上行报文中取出桩编号字节数组
        byte[] pileCodeBytes = Arrays.copyOfRange(uplinkRawFrame, 6, 13);

        // 创建ACK消息体7字节桩编号+2字节计费模型编号+1字节验证结果
        ByteBuf verifyPricingAckMsgBody = Unpooled.buffer(10);
        verifyPricingAckMsgBody.writeBytes(pileCodeBytes);
        verifyPricingAckMsgBody.writeBytes(encodePricingId(verifyPricingResponse.getPricingId()));
        verifyPricingAckMsgBody.writeByte(verifyPricingResponse.getSuccess() ? SUCCESS_BYTE : FAILURE_BYTE);

        encodeAndWriteFlush(VERIFY_PRICING_ACK,
                requestData.getSequenceNumber(),
                requestData.getEncryptionFlag(),
                verifyPricingAckMsgBody,
                tcpSession);
    }
}