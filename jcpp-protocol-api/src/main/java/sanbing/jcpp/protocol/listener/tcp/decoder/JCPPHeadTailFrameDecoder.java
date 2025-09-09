/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.listener.tcp.decoder;

import cn.hutool.core.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static sanbing.jcpp.protocol.listener.tcp.enums.ReadAct.BREAK;
import static sanbing.jcpp.protocol.listener.tcp.enums.ReadAct.CONTINUE;

/**
 * 起始域结束域拆包
 *
 * @author 九筒
 */
@Slf4j
public class JCPPHeadTailFrameDecoder extends ByteToMessageDecoder {
    /**
     * 起始域
     */
    private final byte[] headBytes;

    /**
     * 结束域
     */
    private final byte[] tailBytes;

    public JCPPHeadTailFrameDecoder(String head, String tail) {
        checkNotNull(head, "head");
        checkNotNull(head, "tail");

        this.headBytes = HexUtil.decodeHex(head);
        this.tailBytes = HexUtil.decodeHex(tail);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.isReadable()) {
            Object decoded = decode(ctx, in);

            if (decoded == null || decoded == BREAK) {
                break;
            }

            if (decoded == CONTINUE) {
                continue;
            }

            out.add(decoded);
        }
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        if (log.isTraceEnabled()) {
            String hexDump = ByteBufUtil.hexDump(in);
            log.trace("{} 开始解析16进制报文：{}", ctx.channel(), hexDump);
        }
        // 剩余可读长度
        int readableBytes = in.readableBytes();

        // 读取到的字节长度小于起始域+结束语长度，则跳过先不处理
        if (readableBytes < headBytes.length + tailBytes.length) {
            log.debug("{} 可读长度过短，因此跳过，可读长度:{}", ctx.channel(), readableBytes);
            return BREAK;
        }

        // byteBuf当前的读索引
        int buffIndex = in.readerIndex();

        // 查看前n个字节判断消息头
        byte[] firstBytes = new byte[headBytes.length];
        for (int i = 0; i < headBytes.length; i++, buffIndex++) {
            firstBytes[i] = in.getByte(buffIndex);
        }

        // 校验起始域，如果不符，则丢弃1字节，直到读取到正确的起始域为止
        if (!Arrays.equals(firstBytes, headBytes)) {
            byte aByte = in.readByte();
            if (log.isDebugEnabled()) {
                log.debug("{} 丢弃1字节 {}", ctx.channel(), String.format("%02X", aByte & 0xFF));
            }
            return CONTINUE;
        }

        // 记住起始字节的位置
        int startIndex = in.readerIndex();

        // 找到结束字节序列
        int endIndex = indexOf(in, tailBytes, headBytes.length);

        if (endIndex < 0) {
            log.debug("{} 未找到结束域索引，因此先跳过", ctx.channel());
            return BREAK;
        }

        // 提取报文
        int length = endIndex + tailBytes.length;
        ByteBuf frame = in.retainedSlice(startIndex, length);
        in.readerIndex(startIndex + length);

        return frame;
    }

    public static int indexOf(ByteBuf in, byte[] bytes, int fromIndex) {
        if (bytes.length == 0) {
            return 0;
        }
        if (in.readableBytes() == 0) {
            return -1;
        }

        int targetCount = bytes.length;
        int readerIndex = in.readerIndex() + fromIndex;
        int writerIndex = in.writerIndex();

        int lastIndex = in.indexOf(readerIndex, writerIndex, bytes[0]);
        while (lastIndex != -1) {
            if (lastIndex + targetCount <= writerIndex) {
                boolean matched = true;
                for (int i = 1; i < targetCount; i++) {
                    if (in.getByte(lastIndex + i) != bytes[i]) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    return lastIndex - in.readerIndex();
                }
            }
            lastIndex = in.indexOf(lastIndex + 1, writerIndex, bytes[0]);
        }

        return -1;
    }
}
