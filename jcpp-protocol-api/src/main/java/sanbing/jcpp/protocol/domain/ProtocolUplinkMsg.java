/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.domain;

import io.netty.buffer.ByteBufUtil;

import java.net.SocketAddress;
import java.util.UUID;

public record ProtocolUplinkMsg<T>(SocketAddress address, UUID id, T data, int size) {

    @Override
    public String toString() {
        if (data instanceof byte[] bytes) {
            return ByteBufUtil.hexDump(bytes);
        } else {
            return data.toString();
        }
    }
}