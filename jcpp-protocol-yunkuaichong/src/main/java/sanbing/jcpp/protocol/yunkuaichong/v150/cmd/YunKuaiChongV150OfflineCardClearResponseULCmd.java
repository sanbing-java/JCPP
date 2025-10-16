/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;


import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.UplinkProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.util.List;
import java.util.Map;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0  离线卡数据清除应答
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x45, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150OfflineCardClearResponseULCmd extends YunKuaiChongUplinkCmdExe {

    private static final Map<Byte, Map<Byte, String>> CLEAR_RESULT;


    static {
        CLEAR_RESULT = Map.of(
            (byte) 0x00,Map.of((byte)0x01,"卡号格式错误"),
            (byte) 0x01,Map.of((byte)0x02,SUCCESS)
        );
    }


    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 离线卡数据清除应答", tcpSession);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(message.getMsgBody());
        // 桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);
        // 清除结果集合
        List<UplinkProto.ClearResult> clearResultList = Lists.newArrayList();
        while (byteBuf.readableBytes() >= 10) {
            // 离线卡物理卡号 8字节long值（小端序）
            long physicalCardNoLong = byteBuf.readLongLE();
            String cardNo = String.valueOf(physicalCardNoLong);
            // 清除标记 0x00 清除失败 0x01 清除成功
            byte clearFlag = byteBuf.readByte();
            // 失败原因 0x01 卡号格式错误 0x02 清除成功
            byte  failureReason = byteBuf.readByte();
            // clearResult
            UplinkProto.ClearResult clearResult = UplinkProto.ClearResult.newBuilder()
                    .setCardNo(cardNo)
                    .setSuccess(clearFlag == 0x01)
                    .setErrorMsg(errorMsg(clearFlag,failureReason))
                    .build();
            // add
            clearResultList.add(clearResult);
        }

        UplinkProto.UplinkQueueMessage queueMessage = uplinkMessageBuilder(pileCode, tcpSession, message)
                .setOfflineCardClearResponse(UplinkProto.OfflineCardClearResponse.newBuilder()
                        .setPileCode(pileCode)
                        .addAllClearResult(clearResultList)
                        .build())
                .build();
        // 转发到后端
        tcpSession.getForwarder().sendMessage(queueMessage);
    }


    private String errorMsg(byte clearResult, byte failureReason) {
        if(clearResult == 0x01) {
            return SUCCESS;
        }
        Map<Byte, String> clearResultMap = CLEAR_RESULT.get(clearResult);
        if(null == clearResultMap) {
            return UNKNOWN_MSG;
        }
        return clearResultMap.getOrDefault(failureReason,UNKNOWN_MSG);
    }



}

