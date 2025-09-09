/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.exception;

import lombok.Getter;

import java.io.Serial;

@Getter
public class JCPPException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private JCPPErrorCode errorCode;

    public JCPPException() {
        super();
    }

    public JCPPException(JCPPErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public JCPPException(String message, JCPPErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public JCPPException(String message, Throwable cause, JCPPErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public JCPPException(Throwable cause, JCPPErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

}
