/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.v340.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.proto.gen.DownlinkProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengDownlinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengDwonlinkMessage;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.REMOTE_STOP_CHARGING;
import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 服务器下发充电桩控制命令
 * 目前只支持停止充电
 */
@Slf4j
@ProtocolCmd(value = 5, protocolNames = {V340})
public class LvnengV340RemoteStopDLCmd extends LvnengDownlinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, LvnengDwonlinkMessage lvnengDwonlinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4 服务器下发充电桩控制命令", tcpSession);

        if (!lvnengDwonlinkMessage.getMsg().hasRemoteStopChargingRequest()) {
            return;

        }


        DownlinkProto.RemoteStopChargingRequest remoteStopChargingRequest = lvnengDwonlinkMessage.getMsg().getRemoteStopChargingRequest();
        String pileCode = remoteStopChargingRequest.getPileCode();
        String gunCode = remoteStopChargingRequest.getGunNo();

        ByteBuf msgBody = Unpooled.buffer(44);
        //1预留
        msgBody.writeShortLE(0x00);
        //2预留
        msgBody.writeShortLE(0x00);
        //3充电枪口
        msgBody.writeByte(Integer.parseInt(gunCode));
        //启始命令地址
        msgBody.writeIntLE(2);
        //命令个数
        msgBody.writeByte(1);
        //命令参数长度
        msgBody.writeShortLE(4);
        //命令参数
        msgBody.writeIntLE(0x55);
        encodeAndWriteFlush(REMOTE_STOP_CHARGING,
                msgBody,
                tcpSession);

    }




}
