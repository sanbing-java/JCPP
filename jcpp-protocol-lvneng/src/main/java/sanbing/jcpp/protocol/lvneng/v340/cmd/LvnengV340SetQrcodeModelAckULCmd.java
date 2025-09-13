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
import org.apache.commons.lang3.StringUtils;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.proto.gen.UplinkProto.SetQrcodeResponse;

import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;

import java.nio.charset.StandardCharsets;

import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 充电桩参数字符形设置应答
 */
@Slf4j
@ProtocolCmd(value = 4, protocolNames = {V340})
public class LvnengV340SetQrcodeModelAckULCmd extends LvnengUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4 充电桩参数字符形设置/查询应答", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(lvnengUplinkMessage.getMsgBody());
        //1预留
        byteBuf.skipBytes(2);
        //2预留
        byteBuf.skipBytes(2);

        //3充电桩编码
        byte[] pileCodeBytes = new byte[32];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = StringUtils.trim(new String(pileCodeBytes, StandardCharsets.US_ASCII));

        //4 0-査询 1-设置
        int type = byteBuf.readByte();

        //5启始地址
        long startAddress = byteBuf.readUnsignedIntLE();

        // 6.设置结果 0x00:成功 0x01:失败
        boolean isSuccess = (byteBuf.readByte() == 0x00);
        //todo 查询应答逻辑没有处理

        // 转发到后端
        SetQrcodeResponse setQrcodeResponse = SetQrcodeResponse.newBuilder()
                .setPileCode(pileCode)
                .setType(type)
                .setStartAddr((int) startAddress)
                .setSuccess(isSuccess)
                .build();
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(setQrcodeResponse.getPileCode(), tcpSession, lvnengUplinkMessage)
                .setSetQrcodeResponse(setQrcodeResponse)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);

    }

}
