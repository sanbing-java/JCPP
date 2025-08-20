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
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.TransactionRecordResponse;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengDownlinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengDwonlinkMessage;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;
import sanbing.jcpp.protocol.lvneng.annotation.LvnengCmd;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static sanbing.jcpp.protocol.lvneng.enums.LvnengDownlinkCmdEnum.TRANSACTION_RECORD_ACK;

/**
 * 绿能3.4 服务器应答订单信息
 */
@Slf4j
@LvnengCmd(201)
public class LvnengV340TransactionRecordAckDLCmd extends LvnengDownlinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, LvnengDwonlinkMessage lvnengDwonlinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4服务器订单应答", tcpSession);

        if (!lvnengDwonlinkMessage.getMsg().hasTransactionRecordResponse()) {
            return;
        }

       TransactionRecordResponse transactionRecordResponse = lvnengDwonlinkMessage.getMsg().getTransactionRecordResponse();

        LvnengUplinkMessage requestData = JacksonUtil.fromBytes(lvnengDwonlinkMessage.getMsg().getRequestData().toByteArray(), LvnengUplinkMessage.class);

        // 获取上行报文
        byte[] uplinkRawFrame = requestData.getRawFrame();

        // 从上行报文中取出桩编号字节数组
        byte[] gunCodeBytes = Arrays.copyOfRange(uplinkRawFrame, 45, 46);
        byte[] indexBytes = Arrays.copyOfRange(uplinkRawFrame, 128, 132);

        ByteBuf pingAckMsgBody = Unpooled.buffer(41);
        //1预留
        pingAckMsgBody.writeShortLE(0x00);
        //2预留
        pingAckMsgBody.writeShortLE(0x00);
        //3充电枪口
        pingAckMsgBody.writeBytes(gunCodeBytes);
        //4流水号
        pingAckMsgBody.writeBytes(encodeTradeNo(transactionRecordResponse.getTradeNo()));
        //5内部索引号
        pingAckMsgBody.writeBytes(indexBytes);
        encodeAndWriteFlush(TRANSACTION_RECORD_ACK,
                pingAckMsgBody,
                tcpSession);
    }

    protected static byte[] encodeTradeNo(String tradeNo) {
        // 将tradeNo 读取到32字节数组内
        byte[] bytes = new byte[32];
        byte[] tradeNoBytes = tradeNo.getBytes(StandardCharsets.US_ASCII);
        int copyLength = Math.min(tradeNoBytes.length, bytes.length);
        System.arraycopy(tradeNoBytes, 0, bytes, 0, copyLength);
        return bytes;
    }


}
