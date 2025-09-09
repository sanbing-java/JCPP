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
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.OfflineCardSyncRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.OFFLINE_CARD_SYNC_REQUEST;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;


/**
 * 云快充1.5.0  离线卡数据同步
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x44, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150OfflineCardSyncRequestDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 离线卡数据同步", tcpSession);

        if (!message.getMsg().hasOfflineCardSyncRequest()) {
            log.error("云快充1.5.0 离线卡数据同步消息体为空");
            return;
        }

        OfflineCardSyncRequest request = message.getMsg().getOfflineCardSyncRequest();

        if (request.getTotal() > 15) {
            log.error("云快充1.5.0 离线卡数据同步 下发卡个数最大支持: 15个当前: {}个", request.getTotal());
            return;
        }
        // 初始化 buf
        ByteBuf msgBody = Unpooled.buffer(bufferInitialCapacity(request));
        msgBody.writeBytes(encodePileCode(request.getPileCode()));
        msgBody.writeIntLE(request.getTotal());
        request.getCardInfoList().forEach(cardInfo -> {
            msgBody.writeBytes(BCDUtil.toBytes(cardInfo.getLogicCardNo()));
            msgBody.writeBytes(BCDUtil.toBytes(cardInfo.getCardNo()));
        });

        super.encodeAndWriteFlush(OFFLINE_CARD_SYNC_REQUEST, msgBody, tcpSession);
    }


    /**
     * 桩编号      BCD 码 7
     * 下发卡个数   BIN 码 1 最大 15 个
     * n卡逻辑卡号   BCD 码 8 离线卡逻辑卡号
     * ........ ........ ........ ........
     * n卡物理卡号 BIN 码 8 离线卡物理卡号
     * @param request request
     * @return bufferInitialCapacity
     */
    private int bufferInitialCapacity(OfflineCardSyncRequest request) {
        return (8 + 8) * request.getCardInfoCount() + 7 + 1;
    }


}
