/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import com.google.common.util.concurrent.ListenableFuture;
import sanbing.jcpp.app.adapter.request.PileCreateRequest;
import sanbing.jcpp.app.adapter.request.PileQueryRequest;
import sanbing.jcpp.app.adapter.request.PileUpdateRequest;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.adapter.response.PileOptionResponse;
import sanbing.jcpp.app.adapter.response.PileWithStatusResponse;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileStatusEnum;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.data.kv.AttributesSaveResult;
import sanbing.jcpp.app.exception.JCPPException;

import java.util.List;
import java.util.UUID;

public interface PileService {
    
    /**
     * 创建充电桩
     */
    Pile createPile(PileCreateRequest request);
    
    /**
     * 根据ID查询充电桩
     */
    Pile findById(UUID id);
    
    /**
     * 更新充电桩
     */
    Pile updatePile(UUID id, PileUpdateRequest request) throws JCPPException;
    
    /**
     * 删除充电桩
     */
    void deletePile(UUID id) throws JCPPException;
    
    /**
     * 分页查询充电桩及状态信息
     */
    PageResponse<PileWithStatusResponse> queryPilesWithStatus(PileQueryRequest request);

    /**
     * 获取充电桩选项列表
     */
    List<PileOptionResponse> getPileOptions();
    
    /**
     * 更新充电桩状态
     * 
     * @param pileId 充电桩ID
     * @param status 新状态
     */
    void updatePileStatus(UUID pileId, PileStatusEnum status);
    
    /**
     * 根据充电桩编码更新状态
     * 
     * @param pileCode 充电桩编码
     * @param status 新状态
     */
    void updatePileStatusByCode(String pileCode, PileStatusEnum status);

    /**
     * 查询所有充电桩
     */
    List<Pile> findAll();

    /**
     * 分页查询充电桩（用于状态清洗等批处理场景）
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 充电桩列表
     */
    List<Pile> findPilesWithPagination(int offset, int limit);
    
    /**
     * 查询充电桩状态
     * 
     * @param pileId 充电桩ID
     * @return 状态字符串，如果不存在返回null
     */
    String findPileStatus(UUID pileId);

    /**
     * 处理充电桩登录后的状态管理（优化版）
     * 执行：更新STATUS为ONLINE → 更新CONNECTED_AT → 更新LAST_ACTIVE_TIME
     *
     * @param pileId 充电桩ID
     * @return 异步操作结果
     */
    ListenableFuture<AttributesSaveResult> handlePileLogin(UUID pileId);

    /**
     * 处理充电桩心跳时的状态管理（优化版）
     * 执行：更新STATUS为ONLINE → 更新LAST_ACTIVE_TIME
     *
     * @param pileId 充电桩ID
     * @return 异步操作结果
     */
    ListenableFuture<AttributesSaveResult> handlePileHeartbeat(UUID pileId);

    /**
     * 处理充电桩会话关闭时的状态管理
     * 
     * @param pileCode 充电桩编码
     */
    void handlePileSessionClose(String pileCode);
}
