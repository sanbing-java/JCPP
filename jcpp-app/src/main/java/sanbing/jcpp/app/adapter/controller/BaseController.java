/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import sanbing.jcpp.app.exception.JCPPErrorCode;
import sanbing.jcpp.app.exception.JCPPErrorResponseHandler;
import sanbing.jcpp.app.exception.JCPPException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基础控制器
 * 提供统一的异常处理机制，所有Controller都应该继承此类
 * 
 * @author 九筒
 */
@Slf4j
public abstract class BaseController {

    @Autowired
    private JCPPErrorResponseHandler errorResponseHandler;

    /**
     * 处理所有通用异常
     */
    @ExceptionHandler(Exception.class)
    public void handleControllerException(Exception e, HttpServletResponse response) {
        log.debug("Processing controller exception: {}", e.getMessage(), e);
        
        JCPPException jcppException = handleException(e);
        
        // 如果是通用错误且有具体的原因异常，则使用原始异常
        if (jcppException.getErrorCode() == JCPPErrorCode.GENERAL && 
            jcppException.getCause() instanceof Exception &&
            Objects.equals(jcppException.getCause().getMessage(), jcppException.getMessage())) {
            e = (Exception) jcppException.getCause();
        } else {
            e = jcppException;
        }
        
        errorResponseHandler.handle(e, response);
    }

    /**
     * 处理JCPPException异常
     * 直接委托给统一的错误处理器
     */
    @ExceptionHandler(JCPPException.class)
    public void handleJCPPException(JCPPException ex, HttpServletResponse response) {
        log.debug("Processing JCPP exception: {}", ex.getMessage(), ex);
        errorResponseHandler.handle(ex, response);
    }

    /**
     * 处理参数校验异常
     * 将Spring的验证异常转换为JCPPException
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationError(MethodArgumentNotValidException validationError, HttpServletResponse response) {
        log.warn("Validation error occurred: {}", validationError.getMessage());
        
        // 提取字段错误信息
        String errorMessage = validationError.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        
        if (errorMessage.isEmpty()) {
            errorMessage = "Validation failed";
        } else {
            errorMessage = "Validation error: " + errorMessage;
        }
        
        JCPPException jcppException = new JCPPException(errorMessage, JCPPErrorCode.BAD_REQUEST_PARAMS);
        handleControllerException(jcppException, response);
    }

    /**
     * 异常转换处理方法
     * 将各种异常转换为JCPPException，统一异常处理流程
     */
    private JCPPException handleException(Exception e) {
        if (e instanceof JCPPException jcppException) {
            return jcppException;
        }
        
        // 处理运行时异常
        if (e instanceof RuntimeException) {
            if (e instanceof IllegalArgumentException) {
                return new JCPPException("Invalid argument: " + e.getMessage(), e, JCPPErrorCode.BAD_REQUEST_PARAMS);
            } else if (e instanceof IllegalStateException) {
                return new JCPPException("Invalid state: " + e.getMessage(), e, JCPPErrorCode.VERSION_CONFLICT);
            } else {
                return new JCPPException("Runtime error: " + e.getMessage(), e, JCPPErrorCode.GENERAL);
            }
        }
        
        // 其他异常统一处理为通用错误
        return new JCPPException("Unexpected error: " + e.getMessage(), e, JCPPErrorCode.GENERAL);
    }
}


