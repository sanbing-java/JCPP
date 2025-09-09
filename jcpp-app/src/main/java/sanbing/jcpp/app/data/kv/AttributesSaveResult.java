/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.data.kv;

import java.util.Collections;
import java.util.List;

public record AttributesSaveResult(List<Integer> versions) {

    public static final AttributesSaveResult EMPTY = new AttributesSaveResult(Collections.emptyList());

    public static AttributesSaveResult of(List<Integer> versions) {
        if (versions == null) {
            return EMPTY;
        }
        return new AttributesSaveResult(versions);
    }

}
