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
import sanbing.jcpp.proto.gen.DownlinkProto.RemoteStopChargingRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.REMOTE_STOP_CHARGING;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0 运营平台远程停机
 *
 * @author 九筒
 */
@Slf4j
@ProtocolCmd(value = 0x36, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150RemoteStopDLCmd extends YunKuaiChongDownlinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0运营平台远程停机", tcpSession);

        if (!yunKuaiChongDwonlinkMessage.getMsg().hasRemoteStopChargingRequest()) {
            return;
        }

        RemoteStopChargingRequest remoteStopChargingRequest = yunKuaiChongDwonlinkMessage.getMsg().getRemoteStopChargingRequest();
        String pileCode = remoteStopChargingRequest.getPileCode();
        String gunCode = remoteStopChargingRequest.getGunNo();

        ByteBuf msgBody = Unpooled.buffer(44);
        // 桩编码
        msgBody.writeBytes(encodePileCode(pileCode));
        // 枪号
        msgBody.writeBytes(encodeGunCode(gunCode));

        encodeAndWriteFlush(REMOTE_STOP_CHARGING,
                msgBody,
                tcpSession);
    }
}