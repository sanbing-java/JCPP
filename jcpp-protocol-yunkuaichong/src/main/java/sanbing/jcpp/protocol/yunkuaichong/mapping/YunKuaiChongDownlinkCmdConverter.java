/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.mapping;

import sanbing.jcpp.protocol.domain.DownlinkCmdEnum;
import sanbing.jcpp.protocol.mapping.DownlinkCmdConverter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 云快充协议下行命令转换器（单例）
 * <p>
 * 建立通用下行命令与云快充协议特定命令字的显式转换关系
 * 使用Map存储转换关系，提供O(1)性能
 * <p>
 * 采用单例模式，避免重复实例化
 *
 * @author 九筒
 * @since 2024-12-16
 */
public class YunKuaiChongDownlinkCmdConverter implements DownlinkCmdConverter {

    /**
     * 单例实例
     */
    private static final YunKuaiChongDownlinkCmdConverter INSTANCE = new YunKuaiChongDownlinkCmdConverter();

    /**
     * 命令映射表，使用Map提供O(1)查找性能
     */
    private static final Map<DownlinkCmdEnum, Integer> COMMAND_MAP = new ConcurrentHashMap<>();

    static {
        // 初始化云快充协议的命令映射
        COMMAND_MAP.put(DownlinkCmdEnum.LOGIN_ACK, 0x02);
        COMMAND_MAP.put(DownlinkCmdEnum.HEARTBEAT_ACK, 0x04);  // 心跳应答
        COMMAND_MAP.put(DownlinkCmdEnum.VERIFY_PRICING_ACK, 0x06);
        COMMAND_MAP.put(DownlinkCmdEnum.QUERY_PRICING_ACK, 0X0A);
        COMMAND_MAP.put(DownlinkCmdEnum.SET_PRICING, 0x58);
        COMMAND_MAP.put(DownlinkCmdEnum.REMOTE_START_CHARGING, 0x34);
        COMMAND_MAP.put(DownlinkCmdEnum.REMOTE_STOP_CHARGING, 0x36);
        COMMAND_MAP.put(DownlinkCmdEnum.TRANSACTION_RECORD_ACK, 0x40);
        COMMAND_MAP.put(DownlinkCmdEnum.REMOTE_PARALLEL_START_CHARGING, 0xA4);
        COMMAND_MAP.put(DownlinkCmdEnum.REMOTE_RESTART_PILE, 0x92);
        COMMAND_MAP.put(DownlinkCmdEnum.OTA_REQUEST, 0x94);
        COMMAND_MAP.put(DownlinkCmdEnum.OFFLINE_CARD_BALANCE_UPDATE_REQUEST, 0x42);
        COMMAND_MAP.put(DownlinkCmdEnum.OFFLINE_CARD_SYNC_REQUEST, 0x44);
        COMMAND_MAP.put(DownlinkCmdEnum.SYNC_TIME_REQUEST, 0x56);
    }

    /**
     * 私有构造函数，防止外部实例化
     */
    private YunKuaiChongDownlinkCmdConverter() {
    }

    /**
     * 获取单例实例
     *
     * @return 单例实例
     */
    public static YunKuaiChongDownlinkCmdConverter getInstance() {
        return INSTANCE;
    }

    @Override
    public Integer convertToCmd(DownlinkCmdEnum downlinkCmd) {
        return COMMAND_MAP.get(downlinkCmd);
    }

    @Override
    public String getProtocolName() {
        return "云快充协议";
    }
}
