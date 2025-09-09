/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import sanbing.jcpp.app.adapter.request.StationCreateRequest;
import sanbing.jcpp.app.adapter.request.StationQueryRequest;
import sanbing.jcpp.app.adapter.request.StationUpdateRequest;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.adapter.response.StationOption;
import sanbing.jcpp.app.dal.entity.Station;
import sanbing.jcpp.app.exception.JCPPException;

import java.util.List;
import java.util.UUID;

/**
 * 充电站服务接口
 * 
 * @author 九筒
 */
public interface StationService {
    
    /**
     * 分页查询充电站
     */
    PageResponse<Station> getStations(StationQueryRequest request);
    
    /**
     * 根据ID获取充电站
     */
    Station getStationById(UUID id);
    
    /**
     * 创建充电站
     */
    Station createStation(StationCreateRequest request);
    
    /**
     * 更新充电站
     */
    Station updateStation(UUID id, StationUpdateRequest request) throws JCPPException;
    
    /**
     * 删除充电站
     */
    void deleteStation(UUID id) throws JCPPException;
    
    /**
     * 获取充电站选项列表（用于下拉选择）
     */
    List<StationOption> getStationOptions();
    
    /**
     * 搜索充电站选项列表（支持关键字搜索和分页）
     */
    List<StationOption> searchStationOptions(String keyword, int page, int size);
}
