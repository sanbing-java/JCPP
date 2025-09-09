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
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;
import static sanbing.jcpp.protocol.listener.tcp.enums.ReadAct.BREAK;
import static sanbing.jcpp.protocol.listener.tcp.enums.ReadAct.CONTINUE;

/**
 * JCPP长度域拆包
 *
 * @author 九筒
 */
@Slf4j
public class JCPPLengthFieldBasedFrameDecoder extends ByteToMessageDecoder {
    /**
     * 起始域
     */
    private final byte[] headBytes;
    private final ByteOrder byteOrder;
    private final int lengthFieldOffset;
    private final int lengthFieldLength;
    private final int lengthFieldEndOffset;
    private final int lengthAdjustment;
    private final int initialBytesToStrip;
    private int frameLengthInt = -1;

    public JCPPLengthFieldBasedFrameDecoder(String head, ByteOrder byteOrder, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
                                            int initialBytesToStrip) {
        checkNotNull(head, "head");

        this.headBytes = HexUtil.decodeHex(head);

        this.byteOrder = checkNotNull(byteOrder, "byteOrder");

        checkPositiveOrZero(lengthFieldOffset, "lengthFieldOffset");

        checkPositiveOrZero(initialBytesToStrip, "initialBytesToStrip");

        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
        this.lengthAdjustment = lengthAdjustment;
        this.initialBytesToStrip = initialBytesToStrip;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
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
        if (log.isDebugEnabled()) {
            String hexDump = ByteBufUtil.hexDump(in);
            log.debug("{} 开始拆解16进制报文：{}", ctx.channel(), hexDump);
        }
        // 帧长
        long frameLength = 0;
        // new frame
        if (frameLengthInt == -1) {
            // 剩余可读长度
            int readableBytes = in.readableBytes();

            // 读取到的字节长度小于长度域结束位置，则跳过先不处理
            if (readableBytes < lengthFieldEndOffset) {
                log.debug("{} 读取到的字节长度小于长度域结束位置，则跳过先不处理 readableBytes:{}", ctx.channel(), readableBytes);
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
                in.skipBytes(1);
                return CONTINUE;
            }

            // 实际长度域位置
            int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;
            frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset);

            // 如果帧长<0，则跳过buf并抛出异常
            if (frameLength < 0) {
                failOnNegativeLengthField(in, frameLength, lengthFieldEndOffset);
            }

            // 帧长 = 调整长度 + 长度域结束位置
            frameLength += lengthAdjustment + lengthFieldEndOffset;

            // 如果帧长<长度与结束位置，则跳过并抛出异常
            if (frameLength < lengthFieldEndOffset) {
                failOnFrameLengthLessThanLengthFieldEndOffset(in, frameLength, lengthFieldEndOffset);
            }

            frameLengthInt = (int) frameLength;
        }

        // frameLengthInt exist , just check buf
        if (in.readableBytes() < frameLengthInt) {
            log.debug("{} 可读长度小于帧长 {}，因此跳过", ctx.channel(), frameLengthInt);
            return BREAK;
        }

        // 初始跳过长度如果大于帧长，则跳过并抛出异常
        if (initialBytesToStrip > frameLengthInt) {
            failOnFrameLengthLessThanInitialBytesToStrip(in, frameLength, initialBytesToStrip);
        }
        in.skipBytes(initialBytesToStrip);

        // extract frame
        int readerIndex = in.readerIndex();
        int actualFrameLength = frameLengthInt - initialBytesToStrip;
        ByteBuf frame = extractFrame(in, readerIndex, actualFrameLength);
        in.readerIndex(readerIndex + actualFrameLength);
        frameLengthInt = -1; // start processing the next frame

        return frame;
    }

    /**
     * 获取未调整的帧长度
     */
    protected long getUnadjustedFrameLength(ByteBuf buf, int offset) {
        long frameLength;
        switch (lengthFieldLength) {
            case 1 -> frameLength = buf.getUnsignedByte(offset);
            case 2 -> {
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    frameLength = buf.getUnsignedShortLE(offset);
                } else {
                    frameLength = buf.getUnsignedShort(offset);
                }
            }
            case 3 -> {
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    frameLength = buf.getUnsignedMediumLE(offset);
                } else {
                    frameLength = buf.getUnsignedMedium(offset);
                }
            }
            case 4 -> {
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    frameLength = buf.getUnsignedIntLE(offset);
                } else {
                    frameLength = buf.getUnsignedInt(offset);
                }
            }
            case 8 -> {
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    frameLength = buf.getLongLE(offset);
                } else {
                    frameLength = buf.getLong(offset);
                }
            }
            default ->
                    throw new DecoderException("unsupported lengthFieldLength: " + lengthFieldLength + " (expected: 1, 2, 3, 4, or 8)");
        }
        return frameLength;
    }

    private static void failOnNegativeLengthField(ByteBuf in, long frameLength, int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException("negative pre-adjustment length field: " + frameLength);
    }

    private static void failOnFrameLengthLessThanLengthFieldEndOffset(ByteBuf in, long frameLength, int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException(
                "Adjusted frame length (" + frameLength + ") is less " + "than lengthFieldEndOffset: " + lengthFieldEndOffset);
    }

    private static void failOnFrameLengthLessThanInitialBytesToStrip(ByteBuf in, long frameLength, int initialBytesToStrip) {
        in.skipBytes((int) frameLength);
        throw new CorruptedFrameException(
                "Adjusted frame length (" + frameLength + ") is less " + "than initialBytesToStrip: " + initialBytesToStrip);
    }

    protected ByteBuf extractFrame(ByteBuf buffer, int index, int length) {
        return buffer.retainedSlice(index, length);
    }

}
