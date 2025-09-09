/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol;

import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.stats.MessagesStats;
import sanbing.jcpp.infrastructure.util.exception.DownlinkException;
import sanbing.jcpp.infrastructure.util.trace.TracerRunnable;
import sanbing.jcpp.protocol.domain.ListenerToHandlerMsg;
import sanbing.jcpp.protocol.domain.SessionToHandlerMsg;
import sanbing.jcpp.protocol.forwarder.Forwarder;

import java.util.UUID;

/**
 * @author 九筒
 */
@Slf4j
public abstract class ProtocolMessageProcessor {
    protected final Forwarder forwarder;
    protected final ProtocolContext protocolContext;

    protected ProtocolMessageProcessor(Forwarder forwarder, ProtocolContext protocolContext) {
        this.forwarder = forwarder;
        this.protocolContext = protocolContext;
    }

    public void uplinkHandleAsync(ListenerToHandlerMsg listenerToHandlerMsg, MessagesStats uplinkMsgStats) {

        UUID id = listenerToHandlerMsg.session().getId();

        protocolContext.getShardingThreadPool().execute(id, new TracerRunnable(() -> {
            try {

                listenerToHandlerMsg.session().setForwarder(forwarder);

                uplinkHandle(listenerToHandlerMsg);

            } catch (Exception e) {

                uplinkMsgStats.incrementFailed();

                log.error("{} 上行消息处理器处理报文异常", listenerToHandlerMsg.session(), e);
            }
        }));
    }

    protected abstract void uplinkHandle(ListenerToHandlerMsg listenerToHandlerMsg);

    /**
     * 下行消息处理入口
     * 负责统一的异常处理和日志记录
     */
    public void downlinkHandle(SessionToHandlerMsg sessionToHandlerMsg, MessagesStats downlinkMsgStats) throws DownlinkException {
        try {

            doDownlinkHandle(sessionToHandlerMsg);

        } catch (Exception e) {

            downlinkMsgStats.incrementFailed();
            
            log.warn("下行消息处理失败，session: {}, 异常信息: {}", sessionToHandlerMsg.session(), e.getMessage(), e);

            throw new DownlinkException(e.getMessage(), e);
        }
    }

    /**
     * 下行消息具体处理逻辑
     * 由各协议的具体实现类重写
     */
    protected abstract void doDownlinkHandle(SessionToHandlerMsg sessionToHandlerMsg);
}