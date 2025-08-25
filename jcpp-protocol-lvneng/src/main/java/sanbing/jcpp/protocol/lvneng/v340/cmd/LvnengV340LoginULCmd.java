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
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.LoginRequest;
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 充电桩签到信息上报
 */
@Slf4j
@ProtocolCmd(value = 106, protocolNames = {V340})
public class LvnengV340LoginULCmd extends LvnengUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4登录认证请求", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(lvnengUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        // 预留
        byteBuf.readShortLE();
        // 预留
        byteBuf.readShortLE();

        // 充电桩编码
        byte[] pileCodeBytes = new byte[32];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = StringUtils.trim(new String(pileCodeBytes, StandardCharsets.US_ASCII));

        //预留
        int flag = byteBuf.readByte();
        additionalInfo.put("标识", flag);

        // 充电桩软件版本 (4字节)
        // 格式: 0x00 0x01 0x0100 表示 V0.1.256
        int major = byteBuf.readUnsignedByte();     // 主版本号
        int minor = byteBuf.readUnsignedByte();     // 次版本号
        int patch = byteBuf.readUnsignedShort();  // 修订版本号
        String version = String.format("V%d.%d.%d", major, minor, patch);
        additionalInfo.put("版本", version);

        // 预留
        byteBuf.skipBytes(10);

        // 充电枪个数
        int gunsNum = byteBuf.readByte();
        additionalInfo.put("充电枪个数", gunsNum);

        // 预留
        byteBuf.skipBytes(6);

        // 当前充电桩时间
        byte[] pileSystemTimeBytes = new byte[8];
        byteBuf.readBytes(pileSystemTimeBytes);
        LocalDateTime pileSystemTime = BCDUtil.bcdToDate(pileSystemTimeBytes);
        additionalInfo.put("当前充电桩时间", pileSystemTime == null ? null : pileSystemTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        // 预留
        byteBuf.readLong();
        byteBuf.readLong();
        byteBuf.readLong();

        long randomNum = byteBuf.readUnsignedIntLE();
        additionalInfo.put("桩生成的随机数", randomNum);

        // 充电桩与服务器通信协议版本 (2字节)
        // 十进制30表示V3.0
        int softVersion = byteBuf.readUnsignedShortLE();
        int softMajor = softVersion / 10;  // 主版本号
        int softMinor = softVersion % 10;  // 次版本号
        additionalInfo.put("桩后台通信协议版本", String.format("V%d.%d", softMajor, softMinor));

        int whiteVersion = byteBuf.readIntLE();
        additionalInfo.put("白名单版本号", whiteVersion);

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

        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(loginRequest.getPileCode(), tcpSession, lvnengUplinkMessage)
                .setLoginRequest(loginRequest)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }
}
