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
import sanbing.jcpp.proto.gen.ProtocolProto.TimeSyncRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.SYNC_TIME_REQUEST;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0对时设置
 *
 * @author 发财
 * @since 1.0.0
 */
@Slf4j
@ProtocolCmd(value = 0x56, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150TimeSyncDLCmd extends YunKuaiChongDownlinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("云快充1.5.0对时设置");
        if (!yunKuaiChongDwonlinkMessage.getMsg().hasTimeSyncRequest()) {
            return;
        }
        TimeSyncRequest timeSyncRequest = yunKuaiChongDwonlinkMessage.getMsg().getTimeSyncRequest();
        String pileCode = timeSyncRequest.getPileCode();
        String time = timeSyncRequest.getTime();
        ByteBuf syncTimeMsgBody = Unpooled.buffer(14);
        syncTimeMsgBody.writeBytes(encodePileCode(pileCode));
        syncTimeMsgBody.writeBytes(CP56Time2aUtil.encode(DateUtil.parseLocalDateTime(time)));
        encodeAndWriteFlush(SYNC_TIME_REQUEST, syncTimeMsgBody, tcpSession);
    }
}
