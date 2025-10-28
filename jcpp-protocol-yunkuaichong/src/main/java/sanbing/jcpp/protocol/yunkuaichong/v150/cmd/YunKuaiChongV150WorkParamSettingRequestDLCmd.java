/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.WORK_PARAM_SETTING_REQUEST;
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
 * 云快充1.5.0  充电桩工作参数设置
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x52, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150WorkParamSettingRequestDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 充电桩工作参数设置", tcpSession);

        if (!message.getMsg().hasWorkParamSettingRequest()) {
            log.error("云快充1.5.0 充电桩工作参数设置消息体为空");
            return;
        }

        DownlinkProto.WorkParamSettingRequest request = message.getMsg().getWorkParamSettingRequest();

        // 初始化 buf
        ByteBuf msgBody = Unpooled.buffer(9);
        msgBody.writeBytes(encodePileCode(request.getPileCode()));
        msgBody.writeByte(request.getAllow()?0x00:0x01);
        msgBody.writeByte(request.getMaxAllowOutPower());

        super.encodeAndWriteFlush(WORK_PARAM_SETTING_REQUEST, msgBody, tcpSession);
    }

}
