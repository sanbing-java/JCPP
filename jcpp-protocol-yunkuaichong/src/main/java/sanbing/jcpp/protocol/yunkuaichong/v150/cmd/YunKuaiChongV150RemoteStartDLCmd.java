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
import sanbing.jcpp.proto.gen.DownlinkProto.RemoteStartChargingRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;

import java.math.BigDecimal;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.REMOTE_START_CHARGING;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0 运营平台远程控制启机
 *
 * @author 九筒
 */
@Slf4j
@ProtocolCmd(value = 0x34, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150RemoteStartDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0运营平台远程控制启机", tcpSession);

        if (!yunKuaiChongDwonlinkMessage.getMsg().hasRemoteStartChargingRequest()) {
            return;
        }

        RemoteStartChargingRequest remoteStartChargingRequest = yunKuaiChongDwonlinkMessage.getMsg().getRemoteStartChargingRequest();
        String pileCode = remoteStartChargingRequest.getPileCode();
        String gunCode = remoteStartChargingRequest.getGunNo();
        String tradeNo = remoteStartChargingRequest.getTradeNo();
        String limitYuan = remoteStartChargingRequest.getLimitYuan();
        
        // 优先使用传入的卡号，如果没有则使用交易流水号生成
        String logicalCardNo = remoteStartChargingRequest.hasLogicalCardNo() ? 
            remoteStartChargingRequest.getLogicalCardNo() : tradeNo;
        String physicalCardNo = remoteStartChargingRequest.hasPhysicalCardNo() ? 
            remoteStartChargingRequest.getPhysicalCardNo() : tradeNo;
            
        ByteBuf msgBody = Unpooled.buffer(44);
        // 交易流水号
        msgBody.writeBytes(encodeTradeNo(tradeNo));
        // 桩编码
        msgBody.writeBytes(encodePileCode(pileCode));
        // 枪号
        msgBody.writeBytes(encodeGunCode(gunCode));
        // 逻辑卡号 BCD码
        msgBody.writeBytes(encodeCardNo(logicalCardNo));
        // 物理卡号
        msgBody.writeBytes(encodeCardNo(physicalCardNo));
        // 账户余额
        msgBody.writeIntLE(new BigDecimal(limitYuan).multiply(new BigDecimal("100")).intValue());

        encodeAndWriteFlush(REMOTE_START_CHARGING,
                msgBody,
                tcpSession);
    }

}