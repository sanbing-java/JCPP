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
import sanbing.jcpp.proto.gen.DownlinkProto.OtaRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.OTA_REQUEST;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;


/**
 * 云快充1.5.0  远程更新
 *
 * @author bawan
 */
@Slf4j
@ProtocolCmd(value = 0x94, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150OtaRequestDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage message, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0 远程更新", tcpSession);

        if (!message.getMsg().hasOtaRequest()) {
            log.error("云快充1.5.0 远程更新消息体为空");
            return;
        }

        // 初始化 buf
        ByteBuf msgBody = Unpooled.buffer(94);
        // buf 转换 - 使用高性能版本直接写入，避免创建中间ByteBuf对象
        OtaRequest request = message.getMsg().getOtaRequest();
        msgBody.writeBytes(encodePileCode(request.getPileCode()));
        msgBody.writeByte(request.getPileModel());
        writeParamFillZero(msgBody, request.getPilePower(), 2);
        writeParamFillZero(msgBody, request.getAddress(), 16);
        writeParamFillZero(msgBody, request.getPort(), 2);
        writeParamFillZero(msgBody, request.getUsername(), 16);
        writeParamFillZero(msgBody, request.getPassword(), 16);
        writeParamFillZero(msgBody, request.getFilePath(), 32);
        msgBody.writeByte(request.getExecutionControl());
        msgBody.writeByte(request.getDownloadTimeout());

        super.encodeAndWriteFlush(OTA_REQUEST, msgBody, tcpSession);
    }

}
