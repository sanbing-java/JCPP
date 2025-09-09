/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import cn.hutool.core.util.RandomUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.CP56Time2aUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.ProtocolProto.LoginResponse;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.listener.tcp.enums.SequenceNumberLength;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDownlinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static sanbing.jcpp.infrastructure.util.config.ThreadPoolConfiguration.PROTOCOL_SESSION_SCHEDULED;
import static sanbing.jcpp.proto.gen.ProtocolProto.SessionCloseReason.SESSION_CLOSE_MANUALLY;
import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.LOGIN_ACK;
import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.SYNC_TIME_REQUEST;
import static sanbing.jcpp.protocol.listener.tcp.TcpSession.SCHEDULE_KEY_AUTO_SYNC_TIME;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage.FAILURE_BYTE;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongDwonlinkMessage.SUCCESS_BYTE;
import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0登录认证应答
 *
 * @author 九筒
 */
@Slf4j
@ProtocolCmd(value = 0x02, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150LoginAckDLCmd extends YunKuaiChongDownlinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongDwonlinkMessage yunKuaiChongDwonlinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0登录认证应答", tcpSession);

        if (!yunKuaiChongDwonlinkMessage.getMsg().hasLoginResponse()) {
            return;
        }

        LoginResponse loginResponse = yunKuaiChongDwonlinkMessage.getMsg().getLoginResponse();

        YunKuaiChongUplinkMessage requestData = JacksonUtil.fromBytes(yunKuaiChongDwonlinkMessage.getMsg().getRequestData().toByteArray(), YunKuaiChongUplinkMessage.class);

        // 获取上行报文
        byte[] uplinkRawFrame = requestData.getRawFrame();
        // 从上行报文中取出桩编号字节数组
        byte[] pileCodeBytes = Arrays.copyOfRange(uplinkRawFrame, 6, 13);

        if (loginResponse.getSuccess()) {

            // 构造并下发登录ACK
            loginAck(tcpSession, pileCodeBytes, requestData, true);

            // 构造定时对时
            registerSyncTimeTask(tcpSession, pileCodeBytes, requestData);

        } else {

            log.info("云快充V1.5登录认证失败，服务端断开连接。 pileCode:{}", loginResponse.getPileCode());

            // 构造并下发登录ACK
            loginAck(tcpSession, pileCodeBytes, requestData, false);

            // 断开连接
            tcpSession.close(SESSION_CLOSE_MANUALLY);
        }
    }

    private void loginAck(TcpSession tcpSession, byte[] pileCodeBytes, YunKuaiChongUplinkMessage requestData, boolean loginSuccess) {
        // 创建ACK消息体7字节桩编号+1字节登录结果
        ByteBuf loginAckMsgBody = Unpooled.buffer(8);
        loginAckMsgBody.writeBytes(pileCodeBytes);
        loginAckMsgBody.writeByte(loginSuccess ? SUCCESS_BYTE : FAILURE_BYTE);

        encodeAndWriteFlush(LOGIN_ACK,
                requestData.getSequenceNumber(),
                requestData.getEncryptionFlag(),
                loginAckMsgBody,
                tcpSession);
    }

    private void registerSyncTimeTask(TcpSession tcpSession, byte[] pileCodeBytes, YunKuaiChongUplinkMessage requestData) {
        tcpSession.addSchedule(SCHEDULE_KEY_AUTO_SYNC_TIME, k -> {
                    log.info("{} 云快充1.5.0开始注册定时对时任务", tcpSession);
                    return PROTOCOL_SESSION_SCHEDULED.scheduleAtFixedRate(() ->
                                    syncTime(tcpSession, pileCodeBytes, requestData),
                            0, RandomUtil.randomInt(420, 480), TimeUnit.MINUTES);
                }
        );
    }

    private void syncTime(TcpSession tcpSession, byte[] pileCodeBytes, YunKuaiChongUplinkMessage requestData) {
        TracerContextUtil.newTracer();
        MDCUtils.recordTracer();
        log.info("{} 云快充1.5.0开始下发对时报文", tcpSession);
        ByteBuf syncTimeMsgBody = Unpooled.buffer(14);
        syncTimeMsgBody.writeBytes(pileCodeBytes);
        syncTimeMsgBody.writeBytes(CP56Time2aUtil.encode(LocalDateTime.now()));

        encodeAndWriteFlush(SYNC_TIME_REQUEST,
                tcpSession.nextSeqNo(SequenceNumberLength.SHORT),
                requestData.getEncryptionFlag(),
                syncTimeMsgBody,
                tcpSession);
    }

}