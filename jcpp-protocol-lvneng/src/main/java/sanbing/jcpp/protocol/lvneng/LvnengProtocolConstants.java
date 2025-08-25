/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng;

/**
 * 绿能协议常量定义
 *
 * @author sanbing
 * @since 2024-12-16
 */
public final class LvnengProtocolConstants {

    private LvnengProtocolConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 协议名称常量
     */
    public static final class ProtocolNames {
        /** 绿能协议 v3.4.0 */
        public static final String LVNENG_V340 = "lvnengV340";

        // 注解专用简短别名
        public static final String V340 = LVNENG_V340;

        private ProtocolNames() {
            // 工具类，禁止实例化
        }
    }
}
