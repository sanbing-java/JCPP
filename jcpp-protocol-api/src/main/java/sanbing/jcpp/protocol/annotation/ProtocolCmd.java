/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.annotation;

import java.lang.annotation.*;

/**
 * 通用协议命令注解
 * 所有协议的命令类都应该使用此注解
 *
 * @author 九筒
 * @since 2024-12-16
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtocolCmd {

    /**
     * 命令字值
     */
    int value();

    /**
     * 支持的协议名列表，必须明确指定
     */
    String[] protocolNames();
}
