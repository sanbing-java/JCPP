/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.LoginRequest;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.nio.charset.StandardCharsets;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0充电桩登录认证
 */
@Slf4j
@ProtocolCmd(value = 0x01, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150LoginULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0登录认证请求", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        int pileType = byteBuf.readUnsignedByte();
        additionalInfo.put("桩类型(0直流1交流)", pileType);

        int gunsNum = byteBuf.readUnsignedByte();
        additionalInfo.put("充电枪数量", gunsNum);
        additionalInfo.put("通信协议版本", byteBuf.readUnsignedByte());
        byte[] bytes = new byte[8];
        byteBuf.readBytes(bytes);
        additionalInfo.put("程序版本", new String(bytes, StandardCharsets.US_ASCII));
        additionalInfo.put("网络链接类型", byteBuf.readUnsignedByte());

        byte[] simB = new byte[10];
        byteBuf.readBytes(simB);
        String sim = BCDUtil.toString(simB);
        additionalInfo.put("Sim卡", sim);
        additionalInfo.put("运营商", byteBuf.readUnsignedByte());

        tcpSession.addPileCode(pileCode);

        // 注册前置会话
        ctx.getProtocolSessionRegistryProvider().register(tcpSession);

        // 转发到后端
        LoginRequest loginRequest = LoginRequest.newBuilder()
                .setPileCode(pileCode)
                .setCredential(pileCode)
                .setRemoteAddress(tcpSession.getAddress().toString())
                .setNodeId(ctx.getServiceInfoProvider().getServiceId())
                .setNodeHostAddress(ctx.getServiceInfoProvider().getHostAddress())
                .setNodeRestPort(ctx.getServiceInfoProvider().getRestPort())
                .setNodeGrpcPort(ctx.getServiceInfoProvider().getGrpcPort())
                .setAdditionalInfo(additionalInfo.toString())
                .build();
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(loginRequest.getPileCode(), tcpSession, yunKuaiChongUplinkMessage)
                .setLoginRequest(loginRequest)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

}
