/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class JCPPCredentialsExpiredResponse extends JCPPErrorResponse {

    @Getter
    private final String resetToken;

    protected JCPPCredentialsExpiredResponse(String message, String resetToken) {
        super(message, JCPPErrorCode.CREDENTIALS_EXPIRED, HttpStatus.UNAUTHORIZED);
        this.resetToken = resetToken;
    }

    public static JCPPCredentialsExpiredResponse of(final String message, final String resetToken) {
        return new JCPPCredentialsExpiredResponse(message, resetToken);
    }

}

