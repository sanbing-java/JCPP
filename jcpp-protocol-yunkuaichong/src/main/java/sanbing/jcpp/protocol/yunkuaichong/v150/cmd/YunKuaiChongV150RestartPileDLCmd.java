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
import sanbing.jcpp.proto.gen.ProtocolProto.RestartPileRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;
import static sanbing.jcpp.protocol.yunkuaichong.enums.YunKuaiChongDownlinkCmdEnum.REMOTE_RESTART_PILE;

/**
 * 云快充1.5.0 运营平台远程重启充电桩
 */
@Slf4j
@YunKuaiChongCmd(value = 0x92, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150RestartPileDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0运营平台远程重启充电桩", tcpSession);

        if (!yunKuaiChongDwonlinkMessage.getMsg().hasRestartPileRequest()) {
            return;
        }

        RestartPileRequest restartPileRequest = yunKuaiChongDwonlinkMessage.getMsg().getRestartPileRequest();
        String pileCode = restartPileRequest.getPileCode();
        int type = restartPileRequest.getType();
        ByteBuf msgBody = Unpooled.buffer(8);
        // 桩编码
        msgBody.writeBytes(encodePileCode(pileCode));
        // 0x01：立即执行 0x02：空闲执行
        msgBody.writeInt(type);
        encodeAndWriteFlush(REMOTE_RESTART_PILE,
                msgBody,
                tcpSession);
    }

}