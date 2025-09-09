/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.lvneng.mapping;

import sanbing.jcpp.protocol.domain.DownlinkCmdEnum;
import sanbing.jcpp.protocol.mapping.DownlinkCmdConverter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 绿能协议下行命令转换器（单例）
 * <p>
 * 建立通用下行命令与绿能协议特定命令字的显式转换关系
 * 使用Map存储转换关系，提供O(1)性能
 * <p>
 * 采用单例模式，避免重复实例化
 *
 * @author 九筒
 * @since 2024-12-16
 */
public class LvnengDownlinkCmdConverter implements DownlinkCmdConverter {

    /**
     * 单例实例
     */
    private static final LvnengDownlinkCmdConverter INSTANCE = new LvnengDownlinkCmdConverter();

    /**
     * 命令映射表，使用Map提供O(1)查找性能
     */
    private static final Map<DownlinkCmdEnum, Integer> COMMAND_MAP = new ConcurrentHashMap<>();

    static {
        // 初始化绿能协议的命令映射
        COMMAND_MAP.put(DownlinkCmdEnum.LOGIN_ACK, 105);
        COMMAND_MAP.put(DownlinkCmdEnum.SYNC_TIME_REQUEST, 3);
        COMMAND_MAP.put(DownlinkCmdEnum.TRANSACTION_RECORD_ACK, 201);
        COMMAND_MAP.put(DownlinkCmdEnum.HEARTBEAT_ACK, 101);  // 心跳应答
        COMMAND_MAP.put(DownlinkCmdEnum.REAL_TIME_DATA_ACK, 103);  // 实时数据应答
        // 绿能协议支持以上命令，其他命令返回null
    }

    /**
     * 私有构造函数，防止外部实例化
     */
    private LvnengDownlinkCmdConverter() {
    }

    /**
     * 获取单例实例
     *
     * @return 单例实例
     */
    public static LvnengDownlinkCmdConverter getInstance() {
        return INSTANCE;
    }

    @Override
    public Integer convertToCmd(DownlinkCmdEnum downlinkCmd) {
        return COMMAND_MAP.get(downlinkCmd);
    }

    @Override
    public String getProtocolName() {
        return "绿能协议";
    }
}
