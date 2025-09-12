/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import sanbing.jcpp.app.data.PileSession;
import sanbing.jcpp.app.service.cache.session.PileSessionCacheKey;
import sanbing.jcpp.infrastructure.cache.CacheValueWrapper;
import sanbing.jcpp.infrastructure.cache.TransactionalCache;
import sanbing.jcpp.infrastructure.queue.discovery.ServiceInfoProvider;
import sanbing.jcpp.proto.gen.DownlinkProto.DownlinkRequestMessage;
import sanbing.jcpp.proto.gen.UplinkProto.LoginRequest;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.adapter.DownlinkController;

import java.util.UUID;

/**
 * @author 九筒
 */
@Slf4j
public abstract class DownlinkCallService {

    @Resource
    protected ServiceInfoProvider serviceInfoProvider;

    @Resource
    protected DownlinkController downlinkController;

    @Resource
    protected TransactionalCache<PileSessionCacheKey, PileSession> pileSessionCache;

    @Value("${cache.type}")
    protected String cacheType;

    protected abstract int determinePort(int restPort, int grpcPort);

    protected abstract void _sendDownlinkMessage(DownlinkRequestMessage downlinkMessage, String nodeIp, int port);

    public void sendDownlinkMessage(DownlinkRequestMessage.Builder downlinkMessageBuilder, String pileCode) {
        CacheValueWrapper<PileSession> pileSessionCacheValueWrapper = pileSessionCache.get(new PileSessionCacheKey(pileCode));

        if (pileSessionCacheValueWrapper == null) {
            log.warn("充电桩会话不存在 {}", pileCode);
            return;
        }

        PileSession pileSession = pileSessionCacheValueWrapper.get();

        UUID protocolSessionId = pileSession.getProtocolSessionId();

        if (downlinkMessageBuilder.getSessionIdMSB() == 0) {
            downlinkMessageBuilder.setSessionIdMSB(protocolSessionId.getMostSignificantBits());
        }
        if (downlinkMessageBuilder.getSessionIdLSB() == 0) {
            downlinkMessageBuilder.setSessionIdLSB(protocolSessionId.getLeastSignificantBits());
        }
        if (downlinkMessageBuilder.getProtocolName() == null) {
            downlinkMessageBuilder.setProtocolName(pileSession.getProtocolName());
        }

        String nodeId = pileSession.getNodeId();
        String nodeIp = pileSession.getNodeIp();
        int nodeRestPort = pileSession.getNodeRestPort();
        int nodeGrpcPort = pileSession.getNodeGrpcPort();

        sendDownlinkMessage(downlinkMessageBuilder, nodeId, nodeIp, nodeRestPort, nodeGrpcPort);
    }

    public void sendDownlinkMessage(DownlinkRequestMessage.Builder downlinkMessageBuilder, UplinkQueueMessage uplinkQueueMessage, LoginRequest loginRequest) {

        if (downlinkMessageBuilder.getSessionIdMSB() == 0) {
            downlinkMessageBuilder.setSessionIdMSB(uplinkQueueMessage.getSessionIdMSB());
        }
        if (downlinkMessageBuilder.getSessionIdLSB() == 0) {
            downlinkMessageBuilder.setSessionIdLSB(uplinkQueueMessage.getSessionIdLSB());
        }
        if (downlinkMessageBuilder.getProtocolName() == null) {
            downlinkMessageBuilder.setProtocolName(uplinkQueueMessage.getProtocolName());
        }

        String nodeId = loginRequest.getNodeId();
        String nodeIp = loginRequest.getNodeHostAddress();
        int nodeRestPort = loginRequest.getNodeRestPort();
        int nodeGrpcPort = loginRequest.getNodeGrpcPort();

        sendDownlinkMessage(downlinkMessageBuilder, nodeId, nodeIp, nodeRestPort, nodeGrpcPort);
    }

    private void sendDownlinkMessage(DownlinkRequestMessage.Builder downlinkMessageBuilder, String nodeId, String nodeIp, int nodeRestPort, int nodeGrpcPort) {
        if (serviceInfoProvider.isMonolith() &&
                ("caffeine".equalsIgnoreCase(cacheType) || serviceInfoProvider.getServiceId().equalsIgnoreCase(nodeId))) {

            downlinkController.onDownlink(downlinkMessageBuilder.build())
                    .setResultHandler(result -> log.debug("下行消息发送完成"));

        } else {
            int port = determinePort(nodeRestPort, nodeGrpcPort);

            _sendDownlinkMessage(downlinkMessageBuilder.build(), nodeIp, port);
        }
    }

}