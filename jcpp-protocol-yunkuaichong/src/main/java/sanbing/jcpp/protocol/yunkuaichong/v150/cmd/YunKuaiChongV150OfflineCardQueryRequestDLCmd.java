/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.OFFLINE_CARD_QUERY_REQUEST;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V150;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V160;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V170;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.proto.gen.DownlinkProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;


/**
 * 云快充1.5.0  离线卡数据查询
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x48, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150OfflineCardQueryRequestDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 离线卡数据查询", tcpSession);

        if (!message.getMsg().hasOfflineCardQueryRequest()) {
            log.error("云快充1.5.0 离线卡数据查询消息体为空");
            return;
        }

        DownlinkProto.OfflineCardQueryRequest request = message.getMsg().getOfflineCardQueryRequest();

        if (request.getTotal()>26 || request.getCardNoCount()>26) {
            log.error("云快充1.5.0 离线卡数据查询 下发卡个数最大支持: 26个当前: {}个", request.getTotal());
            return;
        }
        // 初始化 buf
        ByteBuf msgBody = Unpooled.buffer(bufferInitialCapacity(request));
        msgBody.writeIntLE(request.getTotal());
        msgBody.writeBytes(encodePileCode(request.getPileCode()));
        request.getCardNoList().forEach(cardNo -> msgBody.writeBytes(encodeCardNo(cardNo)));

        super.encodeAndWriteFlush(OFFLINE_CARD_QUERY_REQUEST, msgBody, tcpSession);
    }


    /**
     * 桩编号      BCD 码 7
     * 下发卡个数   BIN 码 1 最大 15 个
     * ........ ........ ........ ........
     * n卡物理卡号 BIN 码 8 离线卡物理卡号
     * @param request request
     * @return bufferInitialCapacity
     */
    private int bufferInitialCapacity(DownlinkProto.OfflineCardQueryRequest request) {
        return 7 + 1 + ( 8 * request.getCardNoCount());
    }


}
