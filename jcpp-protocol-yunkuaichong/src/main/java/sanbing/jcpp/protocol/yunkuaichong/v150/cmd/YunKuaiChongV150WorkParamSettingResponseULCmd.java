/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;


import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V150;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V160;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.V170;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.proto.gen.UplinkProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

/**
 * 云快充1.5.0  充电桩工作参数设置应答
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x51, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150WorkParamSettingResponseULCmd extends YunKuaiChongUplinkCmdExe {


    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 充电桩工作参数设置应答", tcpSession);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(message.getMsgBody());
        // 桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);
        // 设置结果
        byte settingResult = byteBuf.readByte();

        UplinkProto.UplinkQueueMessage queueMessage = uplinkMessageBuilder(pileCode, tcpSession, message)
                .setWorkParamSettingResponse(UplinkProto.WorkParamSettingResponse.newBuilder()
                        .setPileCode(pileCode)
                        .setSuccess(settingResult==0x01)
                        .build())
                .build();
        // 转发到后端
        tcpSession.getForwarder().sendMessage(queueMessage);
    }



}

