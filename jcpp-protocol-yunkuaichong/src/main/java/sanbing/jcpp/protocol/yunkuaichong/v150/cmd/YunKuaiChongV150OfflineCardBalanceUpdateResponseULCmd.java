/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import cn.hutool.core.text.CharSequenceUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.OfflineCardBalanceUpdateResponse;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.util.Map;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;


/**
 * 云快充1.5.0  余额更新应答
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x41, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150OfflineCardBalanceUpdateResponseULCmd extends YunKuaiChongUplinkCmdExe {

    private static final Map<Byte, String> UPDATE_RESULT;

    static {
        UPDATE_RESULT = Map.of(
            (byte) 0x00,"修改成功",
            (byte) 0x01,"设备编号错误",
            (byte) 0x02,"卡号错误"
        );
    }



    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 余额更新应答", tcpSession);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(message.getMsgBody());
        // 桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 物理卡号
        String cardNo = CharSequenceUtil.EMPTY;
        if(byteBuf.readableBytes() >= 8) {
            byte[] cardNoBytes = new byte[8];
            byteBuf.readBytes(cardNoBytes);
            cardNo = BCDUtil.toString(cardNoBytes);
        }

        // 修改结果  0x00-修改成功 0x01-设备编号错误 0x02-卡号错误
        byte updateResult = byteBuf.readByte();

        UplinkQueueMessage queueMessage = uplinkMessageBuilder(pileCode, tcpSession, message)
                .setOfflineCardBalanceUpdateResponse(OfflineCardBalanceUpdateResponse.newBuilder()
                        .setPileCode(pileCode)
                        .setCardNo(cardNo)
                        .setSuccess(updateResult == 0x00)
                        .setErrorMsg(UPDATE_RESULT.getOrDefault(updateResult,UNKNOWN_MSG))
                        .build())
                .build();
        // 转发到后端
        tcpSession.getForwarder().sendMessage(queueMessage);
    }

}
