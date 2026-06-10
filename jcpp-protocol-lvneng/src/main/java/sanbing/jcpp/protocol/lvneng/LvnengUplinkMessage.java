/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.lvneng;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class LvnengUplinkMessage implements Serializable {
    // 消息ID
    private final UUID id;

    // 起始域
    private int head;

    // 数据长度
    private int dataLength;

    // 序列号
    private int sequenceNumber;

    // 加密标识
    private int encryptionFlag;

    // 指令
    private int cmd;

    // 消息体
    private byte[] msgBody;

    // 校验和
    private int checkSum;

    // 真实报文
    private byte[] rawFrame;

    public LvnengUplinkMessage(UUID id) {
        this.id = id;
    }

    public LvnengUplinkMessage() {
        this(UUID.randomUUID());
    }

}
