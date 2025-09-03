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
import sanbing.jcpp.proto.gen.DownlinkProto.QrcodeModelProto;
import sanbing.jcpp.proto.gen.DownlinkProto.SetQrcodeRequest;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengDownlinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengDwonlinkMessage;
import sanbing.jcpp.protocol.lvneng.enums.LvnengGunCodeNameEnum;

import java.nio.charset.StandardCharsets;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.SET_QRCODE;
import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 服务器下发充电桩字符型参数
 */
@Slf4j
@ProtocolCmd(value = 3, protocolNames = {V340})
public class LvnengV340SetQrcodeModelDLCmd extends LvnengDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, LvnengDwonlinkMessage lvnengDwonlinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4 服务器下发充电桩字符型参数", tcpSession);

        if (!lvnengDwonlinkMessage.getMsg().hasSetQrcodeRequest()) {
            return;
        }

        SetQrcodeRequest setQrcodeRequest = lvnengDwonlinkMessage.getMsg().getSetQrcodeRequest();
        QrcodeModelProto qrcodeModel = setQrcodeRequest.getQrcodeModel();
        Integer parameterAddress = LvnengGunCodeNameEnum.getParameterAddress(qrcodeModel.getGunName());
        if (parameterAddress == null) {
            log.error("{} 充电桩参数地址不存在", qrcodeModel.getGunName());
            return;
        }

        ByteBuf msgBody = Unpooled.buffer(267);
        // 预留1
        msgBody.writeShortLE(0);
        // 预留1
        msgBody.writeShortLE(0);
        //0-查询 1-设置
        msgBody.writeByte(1);
        // 4 参数起始地址，子命令
        msgBody.writeIntLE(parameterAddress);
        //6  参数字节长度
        msgBody.writeShortLE(256);
        //7  命令参数数据
        msgBody.writeBytes(qrcodeModel.getCode().getBytes(StandardCharsets.US_ASCII));
        //获取二维码参数地址


        // 放进缓存后再下发
        //  tcpSession.getRequestCache().put(pileCode + LvnengDownlinkCmdConverter.getInstance().convertToCmd(SET_PRICING), pricingId);

        encodeAndWriteFlush(SET_QRCODE,
                msgBody,
                tcpSession);
    }


}
