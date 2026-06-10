/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.exception;

import org.springframework.http.HttpStatus;

public class JCPPCredentialsViolationResponse extends JCPPErrorResponse {

    protected JCPPCredentialsViolationResponse(String message) {
        super(message, JCPPErrorCode.PASSWORD_VIOLATION, HttpStatus.UNAUTHORIZED);
    }

    public static JCPPCredentialsViolationResponse of(final String message) {
        return new JCPPCredentialsViolationResponse(message);
    }

}
