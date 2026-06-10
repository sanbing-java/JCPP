/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.validation;

import sanbing.jcpp.infrastructure.util.exception.IncorrectParameterException;

import java.util.UUID;
import java.util.function.Function;

public class Validator {

    public static void validateString(String val, String errorMessage) {
        if (val == null || val.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    public static void validateString(String val, Function<String, String> errorMessageFunction) {
        if (val == null || val.isEmpty()) {
            throw new IncorrectParameterException(errorMessageFunction.apply(val));
        }
    }

    public static void validatePositiveNumber(long val, String errorMessage) {
        if (val <= 0) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    public static void validateId(UUID id, Function<UUID, String> errorMessageFunction) {
        if (id == null) {
            throw new IncorrectParameterException(errorMessageFunction.apply(id));
        }
    }

    public static void checkNotNull(Object reference, String errorMessage) {
        if (reference == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

}
