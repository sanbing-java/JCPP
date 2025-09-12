/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;

/**
 * @author 九筒
 */
@Slf4j
public abstract class LvnengUplinkCmdExe extends AbstractLvnengCmdExe {

    public abstract void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx);

    protected UplinkQueueMessage.Builder uplinkMessageBuilder(String messageKey, TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage) {
        // 从Tracer总获取当前时间
        long ts = TracerContextUtil.getCurrentTracer().getTracerTs();

        return UplinkQueueMessage.newBuilder()
                .setMessageIdMSB(lvnengUplinkMessage.getId().getMostSignificantBits())
                .setMessageIdLSB(lvnengUplinkMessage.getId().getLeastSignificantBits())
                .setSessionIdMSB(tcpSession.getId().getMostSignificantBits())
                .setSessionIdLSB(tcpSession.getId().getLeastSignificantBits())
                .setTs(ts)
                .setRequestData(ByteString.copyFrom(JacksonUtil.writeValueAsBytes(lvnengUplinkMessage)))
                .setMessageKey(messageKey)
                .setProtocolName(tcpSession.getProtocolName());
    }

}