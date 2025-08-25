package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import cn.hutool.core.date.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.codec.CP56Time2aUtil;
import sanbing.jcpp.proto.gen.ProtocolProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;

import java.time.LocalDateTime;

/**
 * 云快充1.5.0 对时结果
 *
 * @author 发财
 * @since 1.0.0
 */
@Slf4j
@YunKuaiChongCmd(0x55)
public class YunKuaiChongV150TimeSyncResultULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 对时结果", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);
        byte[] timeBytes = new byte[7];
        byteBuf.readBytes(timeBytes);
        LocalDateTime dateTime = CP56Time2aUtil.decode(timeBytes);
        String time = DateUtil.formatLocalDateTime(dateTime);
        ProtocolProto.TimeSyncResponse timeSyncResponse = ProtocolProto.TimeSyncResponse.newBuilder()
                .setPileCode(pileCode)
                .setTime(time)
                .build();
        ProtocolProto.UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                .setTimeSyncResponse(timeSyncResponse)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }
}
