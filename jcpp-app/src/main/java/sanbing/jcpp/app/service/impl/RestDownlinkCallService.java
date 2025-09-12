/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sanbing.jcpp.app.service.DownlinkCallService;
import sanbing.jcpp.infrastructure.util.trace.TracerContextUtil;
import sanbing.jcpp.proto.gen.DownlinkProto.DownlinkRequestMessage;

import static sanbing.jcpp.infrastructure.util.trace.TracerContextUtil.*;

/**
 * @author 九筒
 */
@Service
@Slf4j
@ConditionalOnExpression("'${service.downlink.rpc.type:null}'=='rest'")
public class RestDownlinkCallService extends DownlinkCallService {

    @Resource
    RestTemplate downlinkRestTemplate;

    @Override
    protected int determinePort(int restPort, int grpcPort) {
        return restPort;
    }

    @Override
    protected void _sendDownlinkMessage(DownlinkRequestMessage downlinkMessage, String nodeIp, int port) {
        try {
            invokeDownlinkRestApi(downlinkMessage, nodeIp, port);
        } catch (RestClientException e) {
            log.error("下行消息发送异常", e);
        }
    }

    private void invokeDownlinkRestApi(DownlinkRequestMessage downlinkRequestMessage, String nodeIp, int port) {
        // 调整参数名确保一致性
        HttpHeaders headers = new HttpHeaders();
        headers.add(JCPP_TRACER_ID, TracerContextUtil.getCurrentTracer().getTraceId());
        headers.add(JCPP_TRACER_ORIGIN, TracerContextUtil.getCurrentTracer().getOrigin());
        headers.add(JCPP_TRACER_TS, String.valueOf(TracerContextUtil.getCurrentTracer().getTracerTs()));
        headers.setContentType(MediaType.parseMediaType("application/x-protobuf"));

        HttpEntity<DownlinkRequestMessage> entity = new HttpEntity<>(downlinkRequestMessage, headers);

        String url = String.format("http://%s:%d/api/onDownlink", nodeIp, port);
        ResponseEntity<?> response = downlinkRestTemplate.postForEntity(url, entity, ResponseEntity.class);
        log.debug("下行消息发送完成 {}", response);
    }
}