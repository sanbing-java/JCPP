/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import com.google.common.net.HostAndPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.service.DownlinkCallService;
import sanbing.jcpp.app.service.grpc.DownlinkGrpcClient;
import sanbing.jcpp.proto.gen.ProtocolProto.DownlinkRequestMessage;
import sanbing.jcpp.proto.gen.ProtocolProto.RequestMsg;

import static sanbing.jcpp.infrastructure.proto.ProtoConverter.toTracerProto;

/**
 * @author 九筒
 */
@Service
@Slf4j
@ConditionalOnExpression("'${service.downlink.rpc.type:null}'=='grpc'")
public class GrpcDownlinkCallService extends DownlinkCallService {

    @Resource
    DownlinkGrpcClient downlinkGrpcClient;

    @Override
    protected int determinePort(int restPort, int grpcPort) {
        return grpcPort;
    }

    @Override
    protected void _sendDownlinkMessage(DownlinkRequestMessage downlinkMessage, String nodeIp, int port) {
        try {
            RequestMsg requestMsg = RequestMsg.newBuilder()
                    .setTs(System.currentTimeMillis())
                    .setTracer(toTracerProto())
                    .setDownlinkRequestMessage(downlinkMessage)
                    .build();

            downlinkGrpcClient.sendDownlinkRequest(HostAndPort.fromParts(nodeIp, port), requestMsg);
        } catch (Exception e) {
            log.error("下行消息发送异常", e);
        }
    }
}