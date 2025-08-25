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
import sanbing.jcpp.proto.gen.ProtocolProto.HeartBeatRequest;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;

import java.nio.charset.StandardCharsets;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.HEARTBEAT_ACK;
import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 充电桩上传心跳包
 */
@Slf4j
@ProtocolCmd(value = 102, protocolNames = {V340})
public class LvnengV340HeartbeatULCmd extends LvnengUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4充电桩上报心跳包请求", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(lvnengUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        //1预留
        byteBuf.skipBytes(2);
        //2预留
        byteBuf.skipBytes(2);

        //3充电桩编码
        byte[] pileCodeBytes = new byte[32];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = StringUtils.trim(new String(pileCodeBytes, StandardCharsets.US_ASCII));

        //4心跳序号
        int flag = byteBuf.readShortLE();
        additionalInfo.put("心跳序号", flag);
        //5预留
        byteBuf.skipBytes(16);


        tcpSession.addPileCode(pileCode);

        // 注册前置会话
        ctx.getProtocolSessionRegistryProvider().register(tcpSession);

        // 转发到后端
        HeartBeatRequest heartBeatRequest = HeartBeatRequest.newBuilder()
                .setPileCode(pileCode)
                .setRemoteAddress(tcpSession.getAddress().toString())
                .setNodeId(ctx.getServiceInfoProvider().getServiceId())
                .setNodeHostAddress(ctx.getServiceInfoProvider().getHostAddress())
                .setNodeRestPort(ctx.getServiceInfoProvider().getRestPort())
                .setNodeGrpcPort(ctx.getServiceInfoProvider().getGrpcPort())
                .setAdditionalInfo(additionalInfo.toString())
                .build();
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(heartBeatRequest.getPileCode(), tcpSession, lvnengUplinkMessage)
                .setHeartBeatRequest(heartBeatRequest)
                .build();

        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
        //服务器应答心跳包信息
        pingAck(tcpSession, flag);
    }

    private void pingAck(TcpSession tcpSession, int flag) {
        ByteBuf pingAckMsgBody = Unpooled.buffer(6);
        pingAckMsgBody.writeShortLE(0);
        pingAckMsgBody.writeShortLE(0);
        pingAckMsgBody.writeShortLE(flag);

        encodeAndWriteFlush(HEARTBEAT_ACK,
                pingAckMsgBody,
                tcpSession);
    }
}
