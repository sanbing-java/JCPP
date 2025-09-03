/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.v340.cmd;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.UplinkProto.RemoteStopChargingResponse;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;

import java.nio.charset.StandardCharsets;

import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 充电桩对服务器控制命令应答
 */
@Slf4j
@ProtocolCmd(value = 6, protocolNames = {V340})
public class LvnengV340RemoteStopAckULCmd extends LvnengUplinkCmdExe {



    @Override
    public void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4 充电桩对服务器控制命令应答", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(lvnengUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        // 从Tracer总获取当前时间
        long ts = TracerContextUtil.getCurrentTracer().getTracerTs();
        //1预留
        byteBuf.skipBytes(2);
        //2预留
        byteBuf.skipBytes(2);

        //3充电桩编码
        byte[] pileCodeBytes = new byte[32];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = StringUtils.trim(new String(pileCodeBytes, StandardCharsets.US_ASCII));


        //4 充电枪口
        int gunCode = byteBuf.readByte();
        additionalInfo.put("充电枪口", gunCode);

        //5 命令启始标志
        long flag = byteBuf.readUnsignedIntLE();
        additionalInfo.put("命令启始标志", flag);

        //6 命令个数
        int paramCount = byteBuf.readByte();
        additionalInfo.put("命令个数", paramCount);

        // 7 命令执行结果 0x00成功 0x01失败
        boolean isSuccess = (byteBuf.readByte() == 0x00);


        tcpSession.addPileCode(pileCode);


        RemoteStopChargingResponse remoteStopChargingResponse = RemoteStopChargingResponse.newBuilder()
                .setPileCode(pileCode)
                .setGunCode(gunCode+"")
                .setSuccess(isSuccess)
                .setAdditionalInfo(additionalInfo.toString())
                .build();

        // 转发到后端
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(pileCode, tcpSession, lvnengUplinkMessage)
                .setRemoteStopChargingResponse(remoteStopChargingResponse)
                .build();

        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);

    }

}
