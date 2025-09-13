/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.listener.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import sanbing.jcpp.proto.gen.DownlinkProto.DownlinkRequestMessage;
import sanbing.jcpp.proto.gen.UplinkProto.SessionCloseReason;
import sanbing.jcpp.protocol.domain.ProtocolSession;
import sanbing.jcpp.protocol.listener.tcp.enums.SequenceNumberLength;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * 设备会话
 *
 * @author 九筒
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class TcpSession extends ProtocolSession {

    public static final String SCHEDULE_KEY_AUTO_SYNC_TIME =  "auto-sync-time";

    private SocketAddress address;

    private ChannelHandlerContext ctx;

    private final Consumer<DownlinkRequestMessage> sendDownlinkConsumer;

    private final Consumer<ByteBuf> writeAndFlushConsumer;

    private final AtomicInteger sequenceNumber = new AtomicInteger(0);

    public int nextSeqNo(SequenceNumberLength sequenceNumberLength) {
        synchronized (sequenceNumber) {
            int result = sequenceNumber.incrementAndGet();
            switch (sequenceNumberLength) {
                case BYTE -> {
                    if (result == 0xFF) {
                        sequenceNumber.set(0);
                    }
                }
                case SHORT -> {
                    if (result == Short.MAX_VALUE) {
                        sequenceNumber.set(0);
                    }
                }
                default -> {
                    if (result == Integer.MAX_VALUE) {
                        sequenceNumber.set(0);
                    }
                }
            }

            return result;
        }
    }

    public TcpSession(String protocolName,
                      Consumer<DownlinkRequestMessage> sendDownlinkConsumer,
                      Consumer<ByteBuf> writeAndFlushConsumer) {
        super(protocolName);
        this.sendDownlinkConsumer = sendDownlinkConsumer;
        this.writeAndFlushConsumer = writeAndFlushConsumer;
    }

    @Override
    public void onDownlink(DownlinkRequestMessage downlinkMsg) {
        sendDownlinkConsumer.accept(downlinkMsg);
    }

    @Override
    public void close(SessionCloseReason reason) {
        super.close(reason);

        if (ctx != null) {
            ctx.flush();
            ctx.close();
        }
    }

    public void writeAndFlush(ByteBuf byteBuf) {
        writeAndFlushConsumer.accept(byteBuf);
    }
    

}
