/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 告警码-故障码定义
 */
@AllArgsConstructor
@Getter
public enum LvnengAlarmCodeEnum {

    CODE_304(304, "通道匹配异常"),
    CODE_305(305, "主输出接触器故障"),
    CODE_307(307, "交流接触器故障"),
    CODE_308(308, "熔断器故障"),
    CODE_309(309, "防雷故障"),
    CODE_310(310, "电子锁故障"),
    CODE_312(312, "充电模块风扇故障"),
    CODE_313(313, "直流输出过压"),
    CODE_314(314, "直流输出欠压"),
    CODE_315(315, "直流输出过流"),
    CODE_316(316, "车辆端口电压负压"),
    CODE_317(317, "交流输入过压"),
    CODE_318(318, "交流输入欠压"),
    CODE_319(319, "交流输入频率过频"),
    CODE_320(320, "交流输入频率欠频"),
    CODE_321(321, "交流输入电压不平衡"),
    CODE_322(322, "交流输入 A 相缺相"),
    CODE_323(323, "交流输入 B 相缺相"),
    CODE_324(324, "交流输入 C 相缺相"),
    CODE_325(325, "交流输入过载"),
    CODE_326(326, "交流输入异常"),
    CODE_327(327, "充电模块输出过压"),
    CODE_328(328, "充电模块过流"),
    CODE_329(329, "充电模块过温"),
    CODE_330(330, "环境温度过温"),
    CODE_331(331, "环境温度过低"),
    CODE_332(332, "系统无可用模块"),
    CODE_333(333, "充电模块命令执行失败"),
    CODE_334(334, "充电控制器通信故障"),
    CODE_335(335, "采集板通讯故障"),
    CODE_336(336, "电表离线"),
    CODE_337(337, "与集控器通信中断"),
    CODE_338(338, "读卡器通信故障"),
    CODE_339(339, "绝缘故障"),
    CODE_340(340, "系统模块混插"),
    CODE_341(341, "系统急停故障"),
    CODE_342(342, "主从通信异常"),
    CODE_343(343, "电表校验错误"),
    CODE_344(344, "系统门磁故障"),
    CODE_345(345, "系统风机故障"),
    CODE_346(346, "并联接触器故障"),
    CODE_347(347, "并联接触器驱动失效"),
    CODE_348(348, "绝缘监测告警"),
    CODE_351(351, "TMU ID 重复"),
    CODE_352(352, "BMS 数据异常"),
    CODE_353(353, "电池单体过压"),
    CODE_354(354, "电池整包过压保护"),
    CODE_355(355, "电池过流保护"),
    CODE_356(356, "电池过充保护"),
    CODE_357(357, "电池电压异常"),
    CODE_358(358, "电池低温保护"),
    CODE_359(359, "BMS 热失控"),
    CODE_360(360, "BMS 辅源异常"),
    CODE_361(361, "CMU 辅源异常"),
    CODE_362(362, "车辆继电器开路保护"),
    CODE_364(364, "TMU 过温"),
    CODE_368(368, "电池电压过高"),
    CODE_369(369, "电池 SOC 过高"),
    CODE_370(370, "电池 SOC 过低"),
    CODE_371(371, "电池充电过流"),
    CODE_372(372, "电池温度过高"),
    CODE_373(373, "电池绝缘故障"),
    CODE_374(374, "电池输出连接器异常"),
    CODE_385(385, "CC1 电压异常"),
    CODE_386(386, "TMU 所有通道不匹配"),
    CODE_387(387, "电表参数不匹配"),
    CODE_388(388, "枪头短路"),
    CODE_389(389, "TMU 条码异常"),
    CODE_390(390, "TMU 风扇故障"),
    CODE_391(391, "充电系统不匹配"),
    CODE_392(392, "模块开启失败"),
    CODE_394(394, "交流接触器反馈故障"),
    CODE_432(432, "湿度告警故障"),
    CODE_433(433, "枪头过温告警"),
    CODE_434(434, "系统过温故障"),
    CODE_435(435, "机柜浸水故障"),
    CODE_436(436, "机柜倾倒故障"),
    CODE_437(437, "机柜烟雾故障"),
    CODE_442(442, "CMU ID 重复"),
    CODE_443(443, "机柜防尘网告警"),
    CODE_506(506, "未定义的错误"),
    CODE_513(513, "IO 板离线"),
    CODE_514(514, "IO 板故障"),
    CODE_515(515, "TMU 互锁异常"),
    CODE_516(516, "输出接触器前级电压异常"),
    CODE_517(517, "模块地址超范围或地址重复"),
    CODE_518(518, "模块槽位不匹配"),
    CODE_519(519, "TMU 急停故障"),
    CODE_520(520, "CMU 门磁故障"),
    CODE_521(521, "TMU 门磁故障"),
    CODE_522(522, "IO 板互锁故障"),
    CODE_523(523, "CMU 互锁异常"),
    CODE_524(524, "CMU 无可用模块故障"),
    CODE_525(525, "交流输入接触器拒动/误动故障"),
    CODE_526(526, "交流输入接触器粘连故障"),
    CODE_528(528, "泄放回路故障"),
    CODE_529(529, "TMU 无模块可用故障"),
    CODE_531(531, "TCU 离线故障"),
    CODE_532(532, "电表分流器反接故障"),
    CODE_533(533, "绝缘检测仪通信故障"),
    CODE_534(534, "终端 TCU 平台类型未配置"),
    CODE_535(535, "车辆接触器粘连"),
    CODE_536(536, "TMU 程序与终端类型不匹配"),
    CODE_537(537, "SW2 与 SW3 拨码设置错误"),
    CODE_538(538, "TMU 拨码地址超范围");

    /**
     * 故障代码
     */
    private final int code;

    /**
     * 故障描述
     */
    private final String description;


    private static final String UNKNOWN_DESC = "未知故障码";
    private static final Map<Integer, LvnengAlarmCodeEnum> CODE_TO_ENUM_MAP = new HashMap<>();

    static {
        for (LvnengAlarmCodeEnum enumValue : LvnengAlarmCodeEnum.values()) {
            CODE_TO_ENUM_MAP.put(enumValue.getCode(), enumValue);
        }
    }

    public static String getByCode(Long code) {
        LvnengAlarmCodeEnum lvnengAlarmCodeEnum = CODE_TO_ENUM_MAP.get(code.intValue());
        if (lvnengAlarmCodeEnum == null) {
            return UNKNOWN_DESC;
        }
        return lvnengAlarmCodeEnum.getDescription();


    }

}