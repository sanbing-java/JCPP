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
 * 充电桩状态枚举
 */
@AllArgsConstructor
@Getter
public enum LvnengGunCodeNameEnum {

    GUN_QR_CODE_1(13, "1号枪二维码"),
    GUN_QR_CODE_2(14, "2号枪二维码"),
    GUN_QR_CODE_3(15, "3号枪二维码"),
    GUN_QR_CODE_4(16, "4号枪二维码"),
    GUN_QR_CODE_5(17, "5号枪二维码"),
    GUN_QR_CODE_6(18, "6号枪二维码"),
    GUN_QR_CODE_7(19, "7号枪二维码"),
    GUN_QR_CODE_8(20, "8号枪二维码"),
    GUN_QR_CODE_9(21, "9号枪二维码"),
    GUN_QR_CODE_10(22, "10号枪二维码"),
    GUN_QR_CODE_11(23, "11号枪二维码"),
    GUN_QR_CODE_12(24, "12号枪二维码"),
    GUN_QR_CODE_13(25, "13号枪二维码"),
    GUN_QR_CODE_14(26, "14号枪二维码"),
    GUN_QR_CODE_15(27, "15号枪二维码"),
    GUN_QR_CODE_16(28, "16号枪二维码"),
    GUN_QR_CODE_17(29, "17号枪二维码"),
    GUN_QR_CODE_18(30, "18号枪二维码"),
    GUN_QR_CODE_19(31, "19号枪二维码"),
    GUN_QR_CODE_20(32, "20号枪二维码"),
    GUN_QR_CODE_21(33, "21号枪二维码"),
    GUN_QR_CODE_22(34, "22号枪二维码"),
    GUN_QR_CODE_23(35, "23号枪二维码"),
    GUN_QR_CODE_24(36, "24号枪二维码"),
    GUN_QR_CODE_25(37, "25号枪二维码"),
    GUN_QR_CODE_26(38, "26号枪二维码"),
    GUN_QR_CODE_27(39, "27号枪二维码"),
    GUN_QR_CODE_28(40, "28号枪二维码"),
    GUN_QR_CODE_29(41, "29号枪二维码"),
    GUN_QR_CODE_30(42, "30号枪二维码"),
    GUN_QR_CODE_31(43, "31号枪二维码"),
    GUN_QR_CODE_32(44, "32号枪二维码");

    /**
     * 参数地址
     */
    private final int parameterAddress;

    /**
     * 枪二维码描述（便于理解枚举含义）
     */
    private final String description;





    private static final String UNKNOWN_DESC = "未知状态";
    private static final Map<String, LvnengGunCodeNameEnum> CODE_TO_ENUM_MAP = new HashMap<>();

    static {
        for (LvnengGunCodeNameEnum enumValue : LvnengGunCodeNameEnum.values()) {
            CODE_TO_ENUM_MAP.put(enumValue.getDescription(), enumValue);
        }
    }

    public static Integer getParameterAddress(String description) {
        LvnengGunCodeNameEnum lvnengPileStatusEnum = CODE_TO_ENUM_MAP.get(description);
        if (lvnengPileStatusEnum == null) {
            return null;
        }
        return lvnengPileStatusEnum.getParameterAddress();


    }

}