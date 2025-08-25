/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.domain;

/**
 * @author baigod
 */
public enum DownlinkCmdEnum {

    LOGIN_ACK,

    VERIFY_PRICING_ACK,

    QUERY_PRICING_ACK,

    SET_PRICING,

    REMOTE_START_CHARGING,

    REMOTE_STOP_CHARGING,

    TRANSACTION_RECORD_ACK,

    REMOTE_PARALLEL_START_CHARGING,

    REMOTE_RESTART_PILE,

    OTA_REQUEST,

    SYNC_TIME_REQUEST,

    OFFLINE_CARD_BALANCE_UPDATE_REQUEST,

    OFFLINE_CARD_SYNC_REQUEST
}