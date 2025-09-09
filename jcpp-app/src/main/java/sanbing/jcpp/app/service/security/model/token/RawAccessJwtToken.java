/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.security.model.token;


import sanbing.jcpp.app.service.security.model.JwtToken;

import java.io.Serializable;

public record RawAccessJwtToken(String token) implements JwtToken, Serializable {

    private static final long serialVersionUID = -797397445703066079L;

}
