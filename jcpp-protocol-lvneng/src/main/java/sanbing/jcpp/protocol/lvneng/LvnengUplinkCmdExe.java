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
import sanbing.jcpp.proto.gen.ProtocolProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;

/**
 * @author baigod
 */
@Slf4j
public abstract class LvnengUplinkCmdExe extends AbstractLvnengCmdExe {

    public abstract void execute(TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage, ProtocolContext ctx);

    protected UplinkQueueMessage.Builder uplinkMessageBuilder(String messageKey, TcpSession tcpSession, LvnengUplinkMessage lvnengUplinkMessage) {
        return UplinkQueueMessage.newBuilder()
                .setMessageIdMSB(lvnengUplinkMessage.getId().getMostSignificantBits())
                .setMessageIdLSB(lvnengUplinkMessage.getId().getLeastSignificantBits())
                .setSessionIdMSB(tcpSession.getId().getMostSignificantBits())
                .setSessionIdLSB(tcpSession.getId().getLeastSignificantBits())
                .setRequestData(ByteString.copyFrom(JacksonUtil.writeValueAsBytes(lvnengUplinkMessage)))
                .setMessageKey(messageKey)
                .setProtocolName(tcpSession.getProtocolName());
    }

}