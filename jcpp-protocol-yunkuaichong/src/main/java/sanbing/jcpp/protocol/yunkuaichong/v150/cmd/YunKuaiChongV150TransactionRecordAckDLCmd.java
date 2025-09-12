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
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.DownlinkProto.TransactionRecordResponse;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.TRANSACTION_RECORD_ACK;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage.FAILURE_BYTE;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage.SUCCESS_BYTE;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0 交易记录确认
 * @author 九筒
 */
@Slf4j
@ProtocolCmd(value = 0x40, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150TransactionRecordAckDLCmd extends YunKuaiChongDownlinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0交易记录确认", tcpSession);

        if (!yunKuaiChongDwonlinkMessage.getMsg().hasTransactionRecordResponse()) {
            return;
        }

        TransactionRecordResponse transactionRecordAck = yunKuaiChongDwonlinkMessage.getMsg().getTransactionRecordResponse();

        YunKuaiChongUplinkMessage requestData = JacksonUtil.fromBytes(yunKuaiChongDwonlinkMessage.getMsg().getRequestData().toByteArray(), YunKuaiChongUplinkMessage.class);

        // 创建ACK消息体16字节交易流水号 + 1字节确认结果
        ByteBuf msgBody = Unpooled.buffer(17);
        msgBody.writeBytes(encodeTradeNo(transactionRecordAck.getTradeNo()));
        msgBody.writeByte(transactionRecordAck.getSuccess() ? SUCCESS_BYTE : FAILURE_BYTE);

        encodeAndWriteFlush(TRANSACTION_RECORD_ACK,
                requestData.getSequenceNumber(),
                requestData.getEncryptionFlag(),
                msgBody,
                tcpSession);
    }
}