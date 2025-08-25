/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong;

/**
 * 云快充协议常量定义
 *
 * @author sanbing
 * @since 2024-12-16
 */
public final class YunKuaiChongProtocolConstants {

    private YunKuaiChongProtocolConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 协议名称常量
     */
    public static final class ProtocolNames {
        /** 云快充协议 v1.5.0 */
        public static final String YUNKUAICHONG_V150 = "yunkuaichongV150";
        
        /** 云快充协议 v1.6.0 */
        public static final String YUNKUAICHONG_V160 = "yunkuaichongV160";
        
        /** 云快充协议 v1.7.0 */
        public static final String YUNKUAICHONG_V170 = "yunkuaichongV170";

        // 注解专用简短别名
        public static final String V150 = YUNKUAICHONG_V150;
        public static final String V160 = YUNKUAICHONG_V160;
        public static final String V170 = YUNKUAICHONG_V170;

        private ProtocolNames() {
            // 工具类，禁止实例化
        }
    }
}
