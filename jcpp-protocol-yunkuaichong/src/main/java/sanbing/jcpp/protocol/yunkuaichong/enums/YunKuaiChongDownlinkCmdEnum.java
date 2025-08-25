/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author baigod
 */
@AllArgsConstructor
@Getter
public enum YunKuaiChongDownlinkCmdEnum {

    LOGIN_ACK(0x02),

    SYNC_TIME(0x56),

    HEARTBEAT(0x04),

    VERIFY_PRICING_ACK(0x06),

    QUERY_PRICING_ACK(0X0A),

    SET_PRICING(0x58),

    REMOTE_START_CHARGING(0x34),

    REMOTE_STOP_CHARGING(0x36),

    TRANSACTION_RECORD_ACK(0x40),

    REMOTE_PARALLEL_START_CHARGING(0xA4),

    REMOTE_RESTART_PILE(0x92),

    OTA_REQUEST(0x94),

    LIMIT_UPDATE_REQUEST(0x42),

    OFFLINE_CARD_SYNC_REQUEST(0x44),


    ;
    private final Integer cmd;

}