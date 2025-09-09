/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.repository.attribute;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.app.data.kv.KvEntry;
import sanbing.jcpp.infrastructure.util.exception.DataValidationException;
import sanbing.jcpp.infrastructure.util.exception.IncorrectParameterException;
import sanbing.jcpp.infrastructure.util.validation.NoXssValidator;
import sanbing.jcpp.infrastructure.util.validation.Validator;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class KvValidator {

    private static final Cache<String, Boolean> validatedKeys;

    static {
        validatedKeys = Caffeine.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .maximumSize(50000).build();
    }

    public static void validate(List<? extends KvEntry> tsKvEntries, boolean valueNoXssValidation) {
        tsKvEntries.forEach(tsKvEntry -> validate(tsKvEntry, valueNoXssValidation));
    }

    public static void validate(KvEntry tsKvEntry, boolean valueNoXssValidation) {
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("键值条目不能为空");
        }

        String key = tsKvEntry.getKey();

        if (StringUtils.isBlank(key)) {
            throw new DataValidationException("键不能为空");
        }

        if (key.length() > 255) {
            throw new DataValidationException("验证错误：键的长度必须小于或等于255");
        }

        Boolean isValid = validatedKeys.asMap().get(key);
        if (isValid == null) {
            isValid = NoXssValidator.isValid(key);
            validatedKeys.put(key, isValid);
        }
        if (!isValid) {
            throw new DataValidationException("验证错误：键的格式不正确");
        }

        if (valueNoXssValidation) {
            Object value = tsKvEntry.getValue();
            if (value instanceof CharSequence || value instanceof JsonNode) {
                if (!NoXssValidator.isValid(value.toString())) {
                    throw new DataValidationException("验证错误：值的格式不正确");
                }
            }
        }
    }

    
    public static void validateId(UUID id) {
        Validator.validateId(id, uuid -> "ID不正确: " + uuid);
    }

    public static void validateAttributeList(List<AttributeKvEntry> kvEntries, boolean valueNoXssValidation) {
        kvEntries.forEach(tsKvEntry -> validateAttribute(tsKvEntry, valueNoXssValidation));
    }

    public static void validateAttribute(AttributeKvEntry kvEntry, boolean valueNoXssValidation) {
        validate(kvEntry, valueNoXssValidation);
        if (kvEntry.getDataType() == null) {
            throw new IncorrectParameterException("键值条目的数据类型不能为空");
        } else {
            Validator.validateString(kvEntry.getKey(), "键值条目错误：键不能为空");
            Validator.validatePositiveNumber(kvEntry.getLastUpdateTs(), "最后更新时间戳错误：时间戳必须为正数");
        }
    }
}
