/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.listener;

import io.micrometer.core.instrument.Timer;
import sanbing.jcpp.infrastructure.stats.MessagesStats;
import sanbing.jcpp.protocol.ProtocolMessageProcessor;

import java.util.concurrent.atomic.AtomicInteger;

public record ChannelHandlerParameter(String protocolName,
                                      ProtocolMessageProcessor protocolMessageProcessor,
                                      AtomicInteger connectionsGauge,
                                      MessagesStats uplinkMsgStats,
                                      MessagesStats downlinkMsgStats,
                                      Timer downlinkTimer) {
}