package sanbing.jcpp.protocol.yunkuaichong;

import org.junit.Test;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;

import static org.junit.Assert.assertEquals;

/**
 * decodeTradeNo 单元测试：验证流水号解码时前导补位 0 的处理。
 *
 * 编码侧（encodeTradeNo）会将流水号左补 0 到 32 位 BCD，因此解码侧需要
 * 去掉全部前导补位 0，同时不能误伤流水号尾部的有效 0。
 *
 * 对应 issue：#4
 */
public class DecodeTradeNoTest {

    @Test
    public void 多个前导补位0应全部去掉() {
        byte[] tradeNo = BCDUtil.toBytes("00001234");
        assertEquals("1234", AbstractYunKuaiChongCmdExe.decodeTradeNo(tradeNo));
    }

    @Test
    public void 尾部的有效0应保留() {
        byte[] tradeNo = BCDUtil.toBytes("12340000");
        assertEquals("12340000", AbstractYunKuaiChongCmdExe.decodeTradeNo(tradeNo));
    }

    @Test
    public void 全部为补位0时应返回空串() {
        byte[] tradeNo = BCDUtil.toBytes("00000000000000000000000000000000");
        assertEquals("", AbstractYunKuaiChongCmdExe.decodeTradeNo(tradeNo));
    }

    @Test
    public void 无前导0的流水号应保持不变() {
        byte[] tradeNo = BCDUtil.toBytes("1234");
        assertEquals("1234", AbstractYunKuaiChongCmdExe.decodeTradeNo(tradeNo));
    }

    @Test
    public void 满32位流水号的补位0应全部去掉() {
        // 模拟编码侧的实际场景：流水号 "1234567890" 左补 0 到 32 位
        byte[] tradeNo = BCDUtil.toBytes(org.apache.commons.lang3.StringUtils.leftPad("1234567890", 32, '0'));
        assertEquals("1234567890", AbstractYunKuaiChongCmdExe.decodeTradeNo(tradeNo));
    }
}
