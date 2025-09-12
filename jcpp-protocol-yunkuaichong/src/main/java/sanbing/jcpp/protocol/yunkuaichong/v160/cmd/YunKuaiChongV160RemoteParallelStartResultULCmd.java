/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v160.cmd;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.UplinkProto.RemoteStartChargingResponse;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V160;

/**
 * 云快充1.6.0 远程并充启机命令回复
 *
 * @author 九筒
 */
@Slf4j
@ProtocolCmd(value = 0xA3, protocolNames = {V160})
public class YunKuaiChongV160RemoteParallelStartResultULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.6.远程并充启机命令回复", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        // 1.交易流水号
        byte[] tradeNoBytes = new byte[16];
        byteBuf.readBytes(tradeNoBytes);
        String tradeNo = decodeTradeNo(tradeNoBytes);

        // 2.桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 3.抢号
        byte gunCodeByte = byteBuf.readByte();
        String gunCode = BCDUtil.toString(gunCodeByte);

        // 4.命令执行结果 0x00失败 0x01成功
        boolean isSuccess = (byteBuf.readByte() == 0x01);

        // 5.失败原因 0无 1设备编码不匹配 2枪已在充电 3设备故障 4设备离线 5未插枪
        byte failReasonByte = byteBuf.readByte();
        String failReason = mapFailCode(failReasonByte);

        // 6.主辅枪标记 0x00 主枪 0x01辅枪
        byte gunFlagByte = byteBuf.readByte();
        String gunFlag = BCDUtil.toString(gunFlagByte);
        additionalInfo.put("主辅枪标记", gunFlag);

        // 7.并充序号，0xA4下发的并充序号
        byte[] parallelSeqNoBytes = new byte[6];
        byteBuf.readBytes(parallelSeqNoBytes);
        String parallelSeqNo = BCDUtil.toString(parallelSeqNoBytes);
        additionalInfo.put("并充序号", parallelSeqNo);

        RemoteStartChargingResponse remoteStartChargingResponse = RemoteStartChargingResponse.newBuilder()
                .setPileCode(pileCode)
                .setGunCode(gunCode)
                .setTradeNo(tradeNo)
                .setSuccess(isSuccess)
                .setFailReason(failReason)
                .setAdditionalInfo(additionalInfo.toString())
                .build();

        // 转发到后端
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                .setRemoteStartChargingResponse(remoteStartChargingResponse)
                .build();

        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

    public static String mapFailCode(byte failCode) {
        return switch (failCode) {
            case 0x00 -> "无";
            case 0x01 -> "设备编号不匹配";
            case 0x02 -> "枪已在充电";
            case 0x03 -> "设备故障";
            case 0x04 -> "设备离线";
            case 0x05 -> "未插枪";
            case 0x33 -> "充电失败"; // 充电失败或其他相关信息
            case 0x34 -> "待启充"; // 示例，处理收到充电命令
            default -> "未知错误代码";
        };
    }
}