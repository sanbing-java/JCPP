/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class JCPPErrorResponse {
    private final HttpStatus status;

    private final String message;

    private final JCPPErrorCode errorCode;

    private final long timestamp;

    protected JCPPErrorResponse(final String message, final JCPPErrorCode errorCode, HttpStatus status) {
        this.message = message;
        this.errorCode = errorCode;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public static JCPPErrorResponse of(final String message, final JCPPErrorCode errorCode, HttpStatus status) {
        return new JCPPErrorResponse(message, errorCode, status);
    }

    public Integer getStatus() {
        return status.value();
    }

}
