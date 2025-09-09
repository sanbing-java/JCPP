/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sanbing.jcpp.app.adapter.request.StationCreateRequest;
import sanbing.jcpp.app.adapter.request.StationQueryRequest;
import sanbing.jcpp.app.adapter.request.StationUpdateRequest;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.adapter.response.StationOption;
import sanbing.jcpp.app.adapter.response.StationPileCascaderOption;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.dal.entity.Station;
import sanbing.jcpp.app.dal.mapper.PileMapper;
import sanbing.jcpp.app.dal.mapper.StationMapper;
import sanbing.jcpp.app.exception.JCPPErrorCode;
import sanbing.jcpp.app.exception.JCPPException;
import sanbing.jcpp.app.service.StationService;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 充电站服务实现
 * 
 * @author 九筒
 */
@Service
@RequiredArgsConstructor
public class DefaultStationService implements StationService {
    
    private final StationMapper stationMapper;
    private final PileMapper pileMapper;
    
    @Override
    public PageResponse<Station> getStations(StationQueryRequest request) {
        QueryWrapper<Station> wrapper = new QueryWrapper<>();
        
        // 添加搜索条件
        if (StringUtils.hasText(request.getStationName())) {
            wrapper.like("station_name", request.getStationName());
        }
        if (StringUtils.hasText(request.getStationCode())) {
            wrapper.like("station_code", request.getStationCode());
        }
        if (StringUtils.hasText(request.getProvince())) {
            wrapper.eq("province", request.getProvince());
        }
        if (StringUtils.hasText(request.getCity())) {
            wrapper.eq("city", request.getCity());
        }

        
        // 添加排序
        if (StringUtils.hasText(request.getSortField())) {
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
            wrapper.orderBy(true, isAsc, request.getSortField());
        } else {
            wrapper.orderByDesc("created_time");
        }
        
        // 分页查询
        Page<Station> page = new Page<>(request.getPage(), request.getSize());
        IPage<Station> result = stationMapper.selectPage(page, wrapper);
        
        return PageResponse.of(result.getRecords(), result.getTotal(), request.getPage(), request.getSize());
    }
    
    @Override
    public Station getStationById(UUID id) {
        return stationMapper.selectById(id);
    }
    
    @Override
    public Station createStation(StationCreateRequest request) {
        Station station = Station.builder()
                .id(UUID.randomUUID())
                .createdTime(LocalDateTime.now())
                .stationName(request.getStationName())
                .stationCode(request.getStationCode())
                .longitude(request.getLongitude())
                .latitude(request.getLatitude())
                .province(request.getProvince())
                .city(request.getCity())
                .county(request.getCounty())
                .address(request.getAddress())
                .additionalInfo(JacksonUtil.newObjectNode())
                .version(1)
                .build();
        
        stationMapper.insert(station);
        return station;
    }
    
    @Override
    public Station updateStation(UUID id, StationUpdateRequest request) throws JCPPException {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new JCPPException("充电站不存在", JCPPErrorCode.ITEM_NOT_FOUND);
        }
        
        station.setUpdatedTime(LocalDateTime.now()); // 更新时设置更新时间
        station.setStationName(request.getStationName());
        station.setLongitude(request.getLongitude());
        station.setLatitude(request.getLatitude());
        station.setProvince(request.getProvince());
        station.setCity(request.getCity());
        station.setCounty(request.getCounty());
        station.setAddress(request.getAddress());
        
        stationMapper.updateById(station);
        return station;
    }
    
    @Override
    public void deleteStation(UUID id) throws JCPPException {
        // 检查充电站是否存在
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new JCPPException("充电站不存在", JCPPErrorCode.ITEM_NOT_FOUND);
        }
        
        // 检查充电站下是否存在充电桩
        long pileCount = pileMapper.countByStationId(id);
        if (pileCount > 0) {
            throw new JCPPException(
                String.format("无法删除充电站[%s]，该充电站下还有 %d 个充电桩，请先删除所有充电桩", 
                    station.getStationName(), pileCount), 
                JCPPErrorCode.VERSION_CONFLICT);
        }
        
        // 执行删除
        int affectedRows = stationMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new JCPPException("删除充电站失败，可能已被其他操作删除", JCPPErrorCode.VERSION_CONFLICT);
        }
    }
    
    @Override
    public List<StationOption> getStationOptions() {
        QueryWrapper<Station> wrapper = new QueryWrapper<>();
        wrapper.select("id", "station_name", "station_code")
               .orderByAsc("station_name");
        
        List<Station> stations = stationMapper.selectList(wrapper);
        
        return stations.stream()
                .map(station -> StationOption.of(
                    station.getId(),
                    station.getStationName(),
                    station.getStationCode()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StationOption> searchStationOptions(String keyword, int page, int size) {
        QueryWrapper<Station> wrapper = new QueryWrapper<>();
        wrapper.select("id", "station_name", "station_code");
        
        // 如果有关键字，按站名或编码模糊搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("station_name", keyword)
                            .or()
                            .like("station_code", keyword));
        }
        
        // 按创建时间倒序排序
        wrapper.orderByDesc("created_time");
        
        // 分页查询
        Page<Station> pageQuery = new Page<>(page, size);
        IPage<Station> result = stationMapper.selectPage(pageQuery, wrapper);
        
        return result.getRecords().stream()
                .map(station -> StationOption.of(
                    station.getId(),
                    station.getStationName(),
                    station.getStationCode()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StationPileCascaderOption> getStationPileCascaderOptions(String keyword) {
        // 查询充电站
        QueryWrapper<Station> stationWrapper = new QueryWrapper<>();
        stationWrapper.select("id", "station_name", "station_code");
        
        // 如果有关键字，按站名或编码模糊搜索
        if (StringUtils.hasText(keyword)) {
            stationWrapper.and(w -> w.like("station_name", keyword)
                                   .or()
                                   .like("station_code", keyword));
        }
        
        stationWrapper.orderByAsc("station_name");
        List<Station> stations = stationMapper.selectList(stationWrapper);
        
        if (stations.isEmpty()) {
            return List.of();
        }
        
        // 查询所有充电桩
        QueryWrapper<Pile> pileWrapper = new QueryWrapper<>();
        pileWrapper.select("id", "pile_name", "pile_code", "station_id")
                   .in("station_id", stations.stream().map(Station::getId).collect(Collectors.toList()))
                   .orderByAsc("pile_name");
        
        List<Pile> piles = pileMapper.selectList(pileWrapper);
        
        // 按充电站ID分组充电桩
        Map<UUID, List<Pile>> pilesByStation = piles.stream()
                .collect(Collectors.groupingBy(Pile::getStationId));
        
        // 构建级联选择器数据
        return stations.stream()
                .map(station -> {
                    List<Pile> stationPiles = pilesByStation.getOrDefault(station.getId(), List.of());
                    
                    List<StationPileCascaderOption> pileOptions = stationPiles.stream()
                            .map(pile -> StationPileCascaderOption.createPileOption(
                                    station.getId(), station.getStationName(), station.getStationCode(),
                                    pile.getId(), pile.getPileName(), pile.getPileCode()
                            ))
                            .collect(Collectors.toList());
                    
                    return StationPileCascaderOption.createStationOption(
                            station.getId(), station.getStationName(), station.getStationCode(), pileOptions
                    );
                })
                .collect(Collectors.toList());
    }
}
