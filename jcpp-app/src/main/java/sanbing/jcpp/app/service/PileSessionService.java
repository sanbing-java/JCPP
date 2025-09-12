/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.data.PileSession;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;

import java.util.List;
import java.util.Optional;

/**
 * PileSession管理服务
 * 负责充电桩会话的生命周期管理
 * <p>
 * 设计理念：
 * - 基于事件驱动的会话管理（登录、心跳、会话关闭）
 * - 依赖缓存TTL自动过期机制，无需定时清理
 * - 缓存中无会话 = 充电桩离线
 *
 * @author 九筒
 */
public interface PileSessionService {

    /**
     * 创建或更新PileSession
     * 智能判断：如果会话存在则更新活跃时间，不存在则创建新会话
     * 适用于登录、心跳等所有场景，最大程度减少Redis交互次数
     *
     * @param uplinkQueueMessage 上行消息
     * @param pile 充电桩实体
     * @param remoteAddress 远程地址
     * @param nodeId 节点ID
     * @param nodeIp 节点IP
     * @param restPort REST端口
     * @param grpcPort GRPC端口
     * @return 创建或更新的PileSession
     */
    PileSession createOrUpdateSession(UplinkQueueMessage uplinkQueueMessage,
                                    Pile pile,
                                    String remoteAddress,
                                    String nodeId,
                                    String nodeIp,
                                    int restPort,
                                    int grpcPort);

    /**
     * 获取PileSession
     *
     * @param pileCode 充电桩编码
     * @return PileSession的可选包装，如果不存在则返回空
     */
    Optional<PileSession> getSession(String pileCode);



    /**
     * 检查是否存在活跃的会话
     *
     * @param pileCode 充电桩编码
     * @return 是否存在活跃会话
     */
    boolean hasActiveSession(String pileCode);

    /**
     * 移除会话
     * 适用于登出、会话超时等场景
     *
     * @param pileCode 充电桩编码
     */
    void removeSession(String pileCode);


    /**
     * 批量检查会话状态
     * 用于系统启动时的状态清洗
     *
     * @param pileCodes 充电桩编码列表
     * @return 存在活跃会话的充电桩编码列表
     */
    List<String> checkActiveSessions(java.util.List<String> pileCodes);


}
