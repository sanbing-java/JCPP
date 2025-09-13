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
import sanbing.jcpp.proto.gen.UplinkProto;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;

import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 充电桩应答服务器设置 24 时电费计价策略信息
 *
 *
 *
 */
@Slf4j
@ProtocolCmd(value = 1104, protocolNames = {V340})
public class LvnengV340SetPricingModelAckULCmd extends LvnengUplinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4 充电桩应答服务器设置 24 时电费计价策略信息", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(lvnengUplinkMessage.getMsgBody());
        // 1.设置结果 0x00:成功 0x01:失败
        boolean isSuccess = (byteBuf.readByte() == 0x00);

        // 转发到后端
        UplinkProto.SetPricingResponse setPricingResponse = UplinkProto.SetPricingResponse.newBuilder()
                .setSuccess(isSuccess)
                .build();
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(setPricingResponse.getPileCode(), tcpSession, lvnengUplinkMessage)
                .setSetPricingResponse(setPricingResponse)
                .build();
        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);

    }

}
