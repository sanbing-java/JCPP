/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.provider;


import sanbing.jcpp.infrastructure.queue.ProtoQueueMsg;
import sanbing.jcpp.infrastructure.queue.QueueConsumer;
import sanbing.jcpp.infrastructure.queue.QueueProducer;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;

public interface AppQueueFactory {

    QueueConsumer<ProtoQueueMsg<UplinkQueueMessage>> createProtocolUplinkMsgConsumer();

    QueueProducer<ProtoQueueMsg<UplinkQueueMessage>> createProtocolUplinkMsgProducer(String topic);
}
