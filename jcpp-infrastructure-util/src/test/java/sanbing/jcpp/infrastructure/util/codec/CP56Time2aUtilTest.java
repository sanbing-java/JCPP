/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.codec;

import cn.hutool.core.util.HexUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

class CP56Time2aUtilTest {

    @Test
    void encodeTest() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 22, 14, 30, 45, 123_000_000);

        byte[] bytes = CP56Time2aUtil.encode(time);

        System.out.println(Arrays.toString(bytes));
        System.out.println(HexUtil.encodeHex(bytes));
        System.out.println(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        LocalDateTime decode = CP56Time2aUtil.decode(bytes);
        System.out.println(decode.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        assert time.equals(decode);
    }

}