/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.v340.cmd;

import cn.hutool.core.util.RandomUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.mdc.MDCUtils;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.DownlinkProto.LoginResponse;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.listener.tcp.enums.SequenceNumberLength;
import sanbing.jcpp.protocol.lvneng.LvnengDownlinkCmdExe;
import sanbing.jcpp.protocol.lvneng.LvnengDwonlinkMessage;
import sanbing.jcpp.protocol.lvneng.LvnengUplinkMessage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static sanbing.jcpp.infrastructure.util.config.ThreadPoolConfiguration.PROTOCOL_SESSION_SCHEDULED;
import static sanbing.jcpp.proto.gen.UplinkProto.SessionCloseReason.SESSION_CLOSE_MANUALLY;
import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.LOGIN_ACK;
import static sanbing.jcpp.protocol.domain.DownlinkCmdEnum.SYNC_TIME_REQUEST;
import static sanbing.jcpp.protocol.listener.tcp.TcpSession.SCHEDULE_KEY_AUTO_SYNC_TIME;
import static sanbing.jcpp.protocol.lvneng.LvnengProtocolConstants.ProtocolNames.V340;

/**
 * 绿能3.4 服务器应答充电桩签到命令
 */
@Slf4j
@ProtocolCmd(value = 105, protocolNames = {V340})
public class LvnengV340LoginAckDLCmd extends LvnengDownlinkCmdExe {
    @Override
    public void execute(TcpSession tcpSession, LvnengDwonlinkMessage lvnengDwonlinkMessage, ProtocolContext ctx) {
        log.debug("{} 绿能3.4登录认证应答", tcpSession);

        if (!lvnengDwonlinkMessage.getMsg().hasLoginResponse()) {
            return;
        }

        LoginResponse loginResponse = lvnengDwonlinkMessage.getMsg().getLoginResponse();

        LvnengUplinkMessage requestData = JacksonUtil.fromBytes(lvnengDwonlinkMessage.getMsg().getRequestData().toByteArray(), LvnengUplinkMessage.class);

        // 获取上行报文
        byte[] uplinkRawFrame = requestData.getRawFrame();
        // 从上行报文中取出桩编号字节数组
        byte[] pileCodeBytes = Arrays.copyOfRange(uplinkRawFrame, 12, 44);
        byte[] randomNumBytes = Arrays.copyOfRange(uplinkRawFrame, 98, 102);

        if (loginResponse.getSuccess()) {

            // 构造并下发登录ACK
            loginAck(tcpSession, requestData, randomNumBytes);

            // 构造定时对时
            registerSyncTimeTask(tcpSession, pileCodeBytes, requestData);

        } else {

            log.info("绿能3.4登录认证失败，服务端断开连接。 pileCode:{}", loginResponse.getPileCode());

            // 构造并下发登录ACK
            loginAck(tcpSession, requestData, new byte[]{0x00, 0x00, 0x00, 0x00});

            // 断开连接
            tcpSession.close(SESSION_CLOSE_MANUALLY);
        }
    }


    private void loginAck(TcpSession tcpSession, LvnengUplinkMessage requestData, byte[] randomNumBytes) {
        // 创建ACK消息体7字节桩编号+1字节登录结果
        ByteBuf loginAckMsgBody = Unpooled.buffer(18);
        loginAckMsgBody.writeShortLE(0x00);
        loginAckMsgBody.writeShortLE(0x00);
        loginAckMsgBody.writeBytes(randomNumBytes);
        loginAckMsgBody.writeByte(0x00);

        encodeAndWriteFlush(LOGIN_ACK,
                requestData.getSequenceNumber(),
                requestData.getEncryptionFlag(),
                loginAckMsgBody,
                tcpSession);
    }

    private void registerSyncTimeTask(TcpSession tcpSession, byte[] pileCodeBytes, LvnengUplinkMessage requestData) {
        tcpSession.addSchedule(SCHEDULE_KEY_AUTO_SYNC_TIME, k -> {
            log.info("{} 绿能3.4开始注册定时对时任务", tcpSession);
                    return PROTOCOL_SESSION_SCHEDULED.scheduleAtFixedRate(() ->
                                    syncTime(tcpSession, pileCodeBytes, requestData),
                            0, RandomUtil.randomInt(420, 480), TimeUnit.MINUTES);
                }
        );
    }

    private void syncTime(TcpSession tcpSession, byte[] pileCodeBytes, LvnengUplinkMessage requestData) {
        TracerContextUtil.newTracer();
        MDCUtils.recordTracer();
        log.info("{} 绿能3.4开始下发对时报文", tcpSession);

        ByteBuf msgBodyBuf = Unpooled.buffer(20);
        // 预留1
        msgBodyBuf.writeShortLE(0);
        // 预留1
        msgBodyBuf.writeShortLE(0);
        msgBodyBuf.writeByte(1);
        // 4 参数起始地址，子命令
        msgBodyBuf.writeIntLE(2);
        //6  参数字节长度
        msgBodyBuf.writeShortLE(8);
        //7  命令参数数据
        msgBodyBuf.writeBytes(BCDUtil.dateToBcd8(LocalDateTime.now()));

        encodeAndWriteFlush(SYNC_TIME_REQUEST,
                tcpSession.nextSeqNo(SequenceNumberLength.SHORT),
                requestData.getEncryptionFlag(),
                msgBodyBuf,
                tcpSession);
    }

}
