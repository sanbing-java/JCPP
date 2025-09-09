/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * XSS攻击防护注解
 * 用于验证字符串是否包含XSS攻击内容
 *
 * @author 九筒
 */
@Documented
@Constraint(validatedBy = NoXssValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoXss {

    String message() default "输入包含潜在的XSS攻击内容";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}