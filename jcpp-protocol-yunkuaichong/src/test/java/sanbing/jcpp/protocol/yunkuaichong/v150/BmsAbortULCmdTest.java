/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.yunkuaichong.v150;

import org.junit.Test;
import sanbing.jcpp.protocol.yunkuaichong.v150.cmd.YunKuaiChongV150BmsAbortULCmd;

public class BmsAbortULCmdTest {

    @Test
    public void test() {
        YunKuaiChongV150BmsAbortULCmd bmsAbortULCmd = new YunKuaiChongV150BmsAbortULCmd();

        /*
         解析中止原因
         0x05 -> 00000101
         */
        System.out.println("BMS中止充电原因: " + bmsAbortULCmd.parseAbortReasons((byte) 0x05));

        /*
         解析故障原因
         0x0005 -> 00000000 00000101
         */
        System.out.println("BMS中止充电故障原因: " + bmsAbortULCmd.parseFaultReasons(new byte[]{(byte)0x00, (byte)0x05}));

        /*
         解析错误原因
         0x01 -> 00000001
         */
        System.out.println("BMS中止充电错误原因: " + bmsAbortULCmd.parseErrorReasons((byte) 0x01));
    }

}
