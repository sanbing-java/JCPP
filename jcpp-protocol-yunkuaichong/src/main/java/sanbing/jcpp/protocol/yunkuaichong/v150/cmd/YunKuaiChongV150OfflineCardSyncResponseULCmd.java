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
import sanbing.jcpp.proto.gen.UplinkProto.OfflineCardSyncResponse;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.util.Map;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;


/**
 * 云快充1.5.0  离线卡数据同步应答
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x43, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150OfflineCardSyncResponseULCmd extends YunKuaiChongUplinkCmdExe {

    private static final Map<Byte, Map<Byte, String>> FAILURE_REASON;

    static {
        FAILURE_REASON = Map.of(
            (byte) 0x00,Map.of((byte)0x01,"卡号格式错误",(byte)0x02,"储存空间不足"),
            (byte) 0x01,Map.of((byte)0x00,SUCCESS)
        );
    }


    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 离线卡数据同步应答", tcpSession);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(message.getMsgBody());
        // 桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 保存结果 0x00-失败 0x01-成功
        byte saveResult = byteBuf.readByte();
        byte failureReason = 0x00;

        if (byteBuf.readableBytes() >= 1) {
            // 失败原因   0x01-卡号格式错误 0x02-储存空间不足
            failureReason = byteBuf.readByte();
        }

        UplinkQueueMessage queueMessage = uplinkMessageBuilder(pileCode, tcpSession, message)
                .setOfflineCardSyncResponse(OfflineCardSyncResponse.newBuilder()
                        .setPileCode(pileCode)
                        .setSuccess(saveResult == 0x01)
                        .setErrorMsg(errorMsg(saveResult, failureReason))
                        .build())
                .build();
        // 转发到后端
        tcpSession.getForwarder().sendMessage(queueMessage);
    }


    private String errorMsg(byte saveResult, byte failureReason) {
        if(saveResult == 0x01) {
            return SUCCESS;
        }
        Map<Byte, String> saveResultMap = FAILURE_REASON.get(saveResult);
        if(null == saveResultMap) {
            return UNKNOWN_MSG;
        }
        return saveResultMap.getOrDefault(failureReason,UNKNOWN_MSG);
    }



}

