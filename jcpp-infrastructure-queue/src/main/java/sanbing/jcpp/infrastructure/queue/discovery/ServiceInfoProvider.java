/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.queue.discovery;


import sanbing.jcpp.proto.gen.ClusterProto;

/**
 * @author 九筒
 */
public interface ServiceInfoProvider {
    String getServiceId();

    String getHostAddress();

    int getRestPort();

    int getGrpcPort();

    String getServiceType();

    boolean isMonolith();

    ClusterProto.ServiceInfo getServiceInfo();

    ClusterProto.ServiceInfo generateNewServiceInfoWithCurrentSystemInfo();

}