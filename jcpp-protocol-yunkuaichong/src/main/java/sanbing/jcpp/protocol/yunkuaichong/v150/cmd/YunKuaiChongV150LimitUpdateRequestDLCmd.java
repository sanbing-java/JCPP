/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import static sanbing.jcpp.protocol.yunkuaichong.enums.YunKuaiChongDownlinkCmdEnum.LIMIT_UPDATE_REQUEST;

import java.math.BigDecimal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.ProtocolProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;


/**
 * 云快充1.5.0  远程账户余额更新
 *
 * @author bawan
 */
@Slf4j
@YunKuaiChongCmd(0x42)
public class YunKuaiChongV150LimitUpdateRequestDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 远程账户余额更新", tcpSession);

        if (!message.getMsg().hasLimitUpdateRequest()) {
            log.error("云快充1.5.0 远程账户余额更新消息体为空");
            return;
        }

        // 初始化 buf
        ByteBuf msgBody = Unpooled.buffer(20);
        ProtocolProto.LimitUpdateRequest request = message.getMsg().getLimitUpdateRequest();
        msgBody.writeBytes(encodePileCode(request.getPileCode()));
        msgBody.writeBytes(encodeGunCode(request.getGunCode()));
        msgBody.writeBytes(BCDUtil.toBytes(request.getCardNo()));
        msgBody.writeIntLE(new BigDecimal(request.getLimitYuan()).movePointRight(2).intValue());

        super.encodeAndWriteFlush(LIMIT_UPDATE_REQUEST, msgBody, tcpSession);
    }

}
