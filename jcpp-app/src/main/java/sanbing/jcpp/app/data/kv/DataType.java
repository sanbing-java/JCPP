/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.data.kv;

import lombok.Getter;

public enum DataType {

    BOOLEAN(0),
    LONG(1),
    DOUBLE(2),
    STRING(3),
    JSON(4);

    @Getter
    private final int protoNumber; // Corresponds to KeyValueType

    DataType(int protoNumber) {
        this.protoNumber = protoNumber;
    }

}
