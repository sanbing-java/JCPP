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
import sanbing.jcpp.proto.gen.DownlinkProto.OfflineCardBalanceUpdateRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;

import java.math.BigDecimal;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.OFFLINE_CARD_BALANCE_UPDATE_REQUEST;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0  远程账户余额更新
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x42, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150OfflineCardBalanceUpdateRequestDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 远程账户余额更新", tcpSession);

        if (!message.getMsg().hasOfflineCardBalanceUpdateRequest()) {
            log.error("云快充1.5.0 远程账户余额更新消息体为空");
            return;
        }

        // 初始化 buf
        ByteBuf msgBody = Unpooled.buffer(20);
        OfflineCardBalanceUpdateRequest request = message.getMsg().getOfflineCardBalanceUpdateRequest();
        msgBody.writeBytes(encodePileCode(request.getPileCode()));
        msgBody.writeBytes(encodeGunCode(request.getGunNo()));
        msgBody.writeBytes(encodeCardNo(request.getCardNo()));
        msgBody.writeIntLE(new BigDecimal(request.getLimitYuan()).movePointRight(2).intValue());

        super.encodeAndWriteFlush(OFFLINE_CARD_BALANCE_UPDATE_REQUEST, msgBody, tcpSession);
    }

}
