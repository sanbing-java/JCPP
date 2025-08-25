/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import cn.hutool.core.date.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.CP56Time2aUtil;
import sanbing.jcpp.proto.gen.ProtocolProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.annotation.YunKuaiChongCmd;

import static sanbing.jcpp.protocol.yunkuaichong.enums.YunKuaiChongDownlinkCmdEnum.SYNC_TIME;

/**
 * 云快充1.5.0对时设置
 *
 * @author 发财
 * @since 1.0.0
 */
@Slf4j
@YunKuaiChongCmd(0x56)
public class YunKuaiChongV150TimeSyncDLCmd extends YunKuaiChongDownlinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("云快充1.5.0对时设置");
        if (!yunKuaiChongDwonlinkMessage.getMsg().hasTimeSyncRequest()) {
            return;
        }
        ProtocolProto.TimeSyncRequest timeSyncRequest = yunKuaiChongDwonlinkMessage.getMsg().getTimeSyncRequest();
        String pileCode = timeSyncRequest.getPileCode();
        String time = timeSyncRequest.getTime();
        ByteBuf syncTimeMsgBody = Unpooled.buffer(14);
        syncTimeMsgBody.writeBytes(encodePileCode(pileCode));
        syncTimeMsgBody.writeBytes(CP56Time2aUtil.encode(DateUtil.parseLocalDateTime(time)));
        encodeAndWriteFlush(SYNC_TIME, syncTimeMsgBody, tcpSession);
    }
}
