/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持的协议定义类
 * 使用常量定义而非枚举，确保注解可以使用编译时常量
 * 
 * @author 九筒
 * @since 2024-12-22
 */
public final class SupportedProtocols {

    private SupportedProtocols() {
        // 工具类，禁止实例化
    }

    // ==================== 协议常量定义 ====================
    
    /** 云快充协议 v1.5.0 */
    public static final String YUNKUAICHONG_V150 = "yunkuaichongV150";
    
    /** 云快充协议 v1.6.0 */
    public static final String YUNKUAICHONG_V160 = "yunkuaichongV160";
    
    /** 云快充协议 v1.7.0 */
    public static final String YUNKUAICHONG_V170 = "yunkuaichongV170";
    
    /** 绿能协议 v3.4.0 */
    public static final String LVNENG_V340 = "lvnengV340";

    // ==================== 协议显示名称映射 ====================
    
    private static final Map<String, String> PROTOCOL_DISPLAY_NAMES = new HashMap<>();
    
    static {
        // 协议ID与显示名称的映射关系，便于代码走读时对照
        PROTOCOL_DISPLAY_NAMES.put(YUNKUAICHONG_V150, "云快充 V1.5.0");
        PROTOCOL_DISPLAY_NAMES.put(YUNKUAICHONG_V160, "云快充 V1.6.0");
        PROTOCOL_DISPLAY_NAMES.put(YUNKUAICHONG_V170, "云快充 V1.7.0");
        PROTOCOL_DISPLAY_NAMES.put(LVNENG_V340, "绿能 V3.4.0");
    }

    // ==================== 工具方法 ====================

    /**
     * 获取所有支持的协议
     * 直接从映射表中获取，无需反射
     */
    public static List<ProtocolInfo> getAllProtocols() {
        List<ProtocolInfo> protocols = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : PROTOCOL_DISPLAY_NAMES.entrySet()) {
            protocols.add(new ProtocolInfo(entry.getKey(), entry.getValue()));
        }
        
        return protocols;
    }

    /**
         * 协议信息封装类
         */
        public record ProtocolInfo(String protocolId, String displayName) {

    }
}