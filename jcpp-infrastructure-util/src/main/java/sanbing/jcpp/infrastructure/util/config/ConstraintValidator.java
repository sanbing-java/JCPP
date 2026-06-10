/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.config;

import com.google.common.collect.Iterators;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.metadata.ConstraintDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.internal.cfg.context.DefaultConstraintMapping;
import org.hibernate.validator.internal.engine.ConfigurationImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import sanbing.jcpp.infrastructure.util.exception.DataValidationException;
import sanbing.jcpp.infrastructure.util.validation.Length;
import sanbing.jcpp.infrastructure.util.validation.StringLengthValidator;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class ConstraintValidator {

    private static Validator fieldsValidator;

    static {
        initializeValidators();
    }

    public static void validateFields(Object data) {
        validateFields(data, "Validation error: ");
    }

    public static void validateFields(Object data, String errorPrefix) {
        Set<ConstraintViolation<Object>> constraintsViolations = fieldsValidator.validate(data);
        if (!constraintsViolations.isEmpty()) {
            throw new DataValidationException(errorPrefix + getErrorMessage(constraintsViolations));
        }
    }

    public static String getErrorMessage(Collection<ConstraintViolation<Object>> constraintsViolations) {
        return constraintsViolations.stream()
                .map(ConstraintValidator::getErrorMessage)
                .distinct().sorted().collect(Collectors.joining(", "));
    }

    public static String getErrorMessage(ConstraintViolation<Object> constraintViolation) {
        ConstraintDescriptor<?> constraintDescriptor = constraintViolation.getConstraintDescriptor();
        String property = (String) constraintDescriptor.getAttributes().get("fieldName");
        if (StringUtils.isEmpty(property) && !(constraintDescriptor.getAnnotation() instanceof AssertTrue)) {
            property = Iterators.getLast(constraintViolation.getPropertyPath().iterator()).toString();
        }

        String error = "";
        if (StringUtils.isNotEmpty(property)) {
            error += property + " ";
        }
        error += constraintViolation.getMessage();
        return error;
    }

    private static void initializeValidators() {
        HibernateValidatorConfiguration validatorConfiguration = Validation.byProvider(HibernateValidator.class).configure();

        ConstraintMapping constraintMapping = getCustomConstraintMapping();
        validatorConfiguration.addMapping(constraintMapping);

        fieldsValidator = validatorConfiguration.buildValidatorFactory().getValidator();
    }

    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.setConfigurationInitializer(configuration -> {
            ((ConfigurationImpl) configuration).addMapping(getCustomConstraintMapping());
        });
        return localValidatorFactoryBean;
    }

    private static ConstraintMapping getCustomConstraintMapping() {
        ConstraintMapping constraintMapping = new DefaultConstraintMapping(null);
        constraintMapping.constraintDefinition(Length.class).validatedBy(StringLengthValidator.class);
        return constraintMapping;
    }

}
