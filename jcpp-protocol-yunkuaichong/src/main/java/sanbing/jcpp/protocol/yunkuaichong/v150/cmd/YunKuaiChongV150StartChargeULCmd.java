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
import org.springframework.util.DigestUtils;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.ProtocolProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.enums.YunKuaiChongStartTypeEnum;
import sanbing.jcpp.protocol.yunkuaichong.enums.YunKuaiChongPasswordRequiredEnum;


import java.nio.charset.StandardCharsets;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0 充电桩主动申请启动充电
 *
 * @author baiban
 */
@Slf4j
@ProtocolCmd(value = 0x31, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150StartChargeULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0充电桩主动申请启动充电", tcpSession);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();

        // 从Tracer中获取当前时间
        long ts = TracerContextUtil.getCurrentTracer().getTracerTs();

        // 桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 枪号
        byte gunCodeByte = byteBuf.readByte();
        String gunCode = BCDUtil.toString(gunCodeByte);

        // 启动方式
        int startTypeCode = byteBuf.readUnsignedByte();
        String startType = YunKuaiChongStartTypeEnum.getValue(startTypeCode);

        // 是否需要密码
        int needPasswordCode = byteBuf.readUnsignedByte();
        boolean needPassword = YunKuaiChongPasswordRequiredEnum.isPasswordRequired(needPasswordCode);

        // 物理卡号
        byte[] cardNoBytes = new byte[8];
        byteBuf.readBytes(cardNoBytes);
        String cardNo = BCDUtil.toString(cardNoBytes);

        // 密码
        byte[] passwordBytes = new byte[16];
        byteBuf.readBytes(passwordBytes);
        String password = DigestUtils.md5DigestAsHex(passwordBytes).substring(8, 24).toLowerCase();

        // VIN码
        byte[] carVinCodeBytes = new byte[17];
        byteBuf.readBytes(carVinCodeBytes);
        // VIN码反序处理
        String carVinCode = reverseVinCode(new String(carVinCodeBytes, StandardCharsets.US_ASCII));

        // 转发到后端
        ProtocolProto.StartChargeRequest startChargingRequest = ProtocolProto.StartChargeRequest.newBuilder()
                .setTs(ts)
                .setPileCode(pileCode)
                .setGunCode(gunCode)
                .setStartType(startType)
                .setNeedPassword(needPassword)
                .setCardNo(cardNo)
                .setPassword(password)
                .setCarVinCode(carVinCode)
                .setAdditionalInfo(additionalInfo.toString())
                .build();

        ProtocolProto.UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(startChargingRequest.getPileCode(), tcpSession, yunKuaiChongUplinkMessage)
                .setStartChargeRequest(startChargingRequest)
                .build();

        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

    /**
     * VIN码反序处理
     * 
     * @param originalVinCode 原始VIN码
     * @return 反序后的VIN码
     */
    private String reverseVinCode(String originalVinCode) {
        if (originalVinCode == null || originalVinCode.trim().isEmpty()) {
            return "";
        }
        
        // 移除末尾的null字符和空格
        String trimmedVin = originalVinCode.trim().replaceAll("\0", "");
        
        if (trimmedVin.isEmpty()) {
            return "";
        }
        
        // 反序VIN码
        return new StringBuilder(trimmedVin).reverse().toString();
    }

}