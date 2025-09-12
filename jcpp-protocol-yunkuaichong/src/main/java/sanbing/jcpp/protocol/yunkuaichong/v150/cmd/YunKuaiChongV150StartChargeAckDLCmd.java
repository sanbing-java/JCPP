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
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.DownlinkProto;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.enums.YunKuaiChongStartChargeFailureReasonEnum;

import java.math.BigDecimal;

import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.START_CHARGE_ACK;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage.FAILURE_BYTE;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage.SUCCESS_BYTE;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0 充电桩主动申请启动充电
 *
 * @author baiban
 */
@Slf4j
@ProtocolCmd(value = 0x32, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150StartChargeAckDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.info("{} 云快充1.5.0运营平台确认启动充电", tcpSession);

        if (!yunKuaiChongDwonlinkMessage.getMsg().hasStartChargeResponse()) {
            return;
        }

        DownlinkProto.StartChargeResponse startChargeResponse = yunKuaiChongDwonlinkMessage.getMsg().getStartChargeResponse();
        String tradeNo = startChargeResponse.getTradeNo();
        String pileCode = startChargeResponse.getPileCode();
        String gunCode = startChargeResponse.getGunCode();
        String logicalCardNo = startChargeResponse.getLogicalCardNo();
        String limitYuan = startChargeResponse.getLimitYuan();
        String failReasonValue = startChargeResponse.getFailReason();
        boolean authSuccess = startChargeResponse.getAuthSuccess();
        int failReasonCode = YunKuaiChongStartChargeFailureReasonEnum.getCode(failReasonValue);

        ByteBuf msgBody = Unpooled.buffer(44);
        // 交易流水号
        msgBody.writeBytes(encodeTradeNo(tradeNo));
        // 桩编码
        msgBody.writeBytes(encodePileCode(pileCode));
        // 枪号
        msgBody.writeBytes(encodeGunCode(gunCode));
        // 逻辑卡号
        msgBody.writeBytes(encodeCardNo(logicalCardNo));
        // 账户余额
        msgBody.writeIntLE(new BigDecimal(limitYuan).multiply(new BigDecimal("100")).intValue());
        // 鉴权成功标志
        msgBody.writeByte(authSuccess ? SUCCESS_BYTE : FAILURE_BYTE);
        // 失败原因
        msgBody.writeByte(failReasonCode);

        YunKuaiChongUplinkMessage requestData = JacksonUtil.fromBytes(yunKuaiChongDwonlinkMessage.getMsg().getRequestData().toByteArray(), YunKuaiChongUplinkMessage.class);

        encodeAndWriteFlush(START_CHARGE_ACK,
                requestData.getSequenceNumber(),
                requestData.getEncryptionFlag(),
                msgBody,
                tcpSession);

        if (!authSuccess) {
            log.info("业务[云快充1.5.0 充电桩主动申请启动充电失败] 失败原因：{}", YunKuaiChongStartChargeFailureReasonEnum.getDescription(failReasonCode));
        }
    }
}
