package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.ProtocolProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;


/**
 * 云快充1.5.0  远程更新应答
 *
 * @author bawan
 */
@Slf4j
@YunKuaiChongCmd(0x93)
public class YunKuaiChongV150RemoteUpdateAckULCmd extends YunKuaiChongUplinkCmdExe {

    private static final Map<Byte, String> UPGRADE_STATUS;

    static {
        UPGRADE_STATUS = Map.of(
            (byte) 0x00,"成功",
            (byte) 0x01,"编号错误",
            (byte) 0x02,"程序与桩型号不符",
            (byte) 0x03, "下载更新文件超时"
        );
    }


    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 远程更新应答", tcpSession);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(message.getMsgBody());
        // 桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 升级状态 // 0x00成功  0x01编号错误 0x01程序与桩型号不符 0x01下载更新文件超时
        byte upgradeStatus = byteBuf.readByte();

        ProtocolProto.UplinkQueueMessage queueMessage = uplinkMessageBuilder(pileCode, tcpSession, message)
                .setOtaResponse(ProtocolProto.OtaResponse.newBuilder()
                        .setPileCode(pileCode)
                        .setSuccess(upgradeStatus == 0x00)
                        .setErrorMsg(UPGRADE_STATUS.get(upgradeStatus))
                        .build())
                .build();
        // 转发到后端
        tcpSession.getForwarder().sendMessage(queueMessage);
    }

}
