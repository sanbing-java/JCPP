/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import sanbing.jcpp.proto.gen.ProtocolProto.DownlinkRequestMessage;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author 九筒
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class LvnengDwonlinkMessage implements Serializable {
    public static final byte SUCCESS_BYTE = 0x00;
    public static final byte FAILURE_BYTE = 0x01;

    // 消息ID
    private UUID id;

    // 请求ID（如有）
    private UUID requestId;

    // 指令
    private int cmd;

    // 消息体
    private DownlinkRequestMessage msg;

    // 上行消息
    private LvnengUplinkMessage requestData;

}