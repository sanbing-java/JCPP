/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.config.ibatis.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * @author 九筒
 */
public enum PileTypeEnum implements IEnum<String> {
    AC,         // 交流充电桩
    DC,         // 直流充电桩
    ;

    @Override
    public String getValue() {
        return name();
    }
}