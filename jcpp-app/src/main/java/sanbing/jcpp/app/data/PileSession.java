/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author 九筒
 */
@Data
public class PileSession implements Serializable {

    private final UUID pileId;

    private final String pileCode;

    private final String protocolName;

    private UUID protocolSessionId;

    private String remoteAddress;

    private String nodeId;

    private String nodeIp;

    private int nodeRestPort;

    private int nodeGrpcPort;



    public PileSession(UUID pileId, String pileCode, String protocolName) {
        this.pileId = pileId;
        this.pileCode = pileCode;
        this.protocolName = protocolName;
    }

    @JsonCreator
    public PileSession(
            @JsonProperty("pileId") UUID pileId,
            @JsonProperty("pileCode") String pileCode,
            @JsonProperty("protocolName") String protocolName,
            @JsonProperty("protocolSessionId") UUID protocolSessionId,
            @JsonProperty("remoteAddress") String remoteAddress,
            @JsonProperty("nodeId") String nodeId,
            @JsonProperty("nodeIp") String nodeIp,
            @JsonProperty("nodeRestPort") int nodeRestPort,
            @JsonProperty("nodeGrpcPort") int nodeGrpcPort) {
        this.pileId = pileId;
        this.pileCode = pileCode;
        this.protocolName = protocolName;
        this.protocolSessionId = protocolSessionId;
        this.remoteAddress = remoteAddress;
        this.nodeId = nodeId;
        this.nodeIp = nodeIp;
        this.nodeRestPort = nodeRestPort;
        this.nodeGrpcPort = nodeGrpcPort;
    }
}