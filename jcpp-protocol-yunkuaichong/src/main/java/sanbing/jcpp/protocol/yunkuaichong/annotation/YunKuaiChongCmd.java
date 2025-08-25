/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.annotation;

import java.lang.annotation.*;

/**
 * @author baigod
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YunKuaiChongCmd {

    int value();

    /**
     * 支持的协议名列表，必须明确指定
     */
    String[] protocolNames();

}