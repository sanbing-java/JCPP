/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.codec;

import cn.hutool.core.util.HexUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BCDUtilTest {

    @Test
    void toBytesTest() {
        String pileCodeHex = "20231212000010";

        byte[] bytes = HexUtil.decodeHex(pileCodeHex);

        String pileCode = BCDUtil.toString(bytes);

        assert pileCodeHex.equals(pileCode);

        byte[] pileCodeBytes = BCDUtil.toBytes(pileCodeHex);

        assertArrayEquals(pileCodeBytes, bytes);
    }
}