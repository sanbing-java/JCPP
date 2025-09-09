/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sanbing.jcpp.app.adapter.request.StationCreateRequest;
import sanbing.jcpp.app.adapter.request.StationQueryRequest;
import sanbing.jcpp.app.adapter.request.StationUpdateRequest;
import sanbing.jcpp.app.adapter.response.ApiResponse;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.adapter.response.StationOption;
import sanbing.jcpp.app.adapter.response.StationPileCascaderOption;
import sanbing.jcpp.app.dal.entity.Station;
import sanbing.jcpp.app.exception.JCPPException;
import sanbing.jcpp.app.service.StationService;

import java.util.List;
import java.util.UUID;

/**
 * 充电站管理控制器
 * 
 * @author 九筒
 */
@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController extends BaseController {
    
    private final StationService stationService;
    
    /**
     * 分页查询充电站
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Station>>> getStations(StationQueryRequest request) {
        PageResponse<Station> result = stationService.getStations(request);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }
    
    /**
     * 根据ID获取充电站详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Station>> getStation(@PathVariable UUID id) {
        Station station = stationService.getStationById(id);
        if (station == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success("查询成功", station));
    }
    
    /**
     * 创建充电站
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Station>> createStation(@Valid @RequestBody StationCreateRequest request) {
        Station station = stationService.createStation(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", station));
    }
    
    /**
     * 更新充电站
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Station>> updateStation(@PathVariable UUID id, 
                                                               @Valid @RequestBody StationUpdateRequest request) throws JCPPException {
        Station station = stationService.updateStation(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", station));
    }
    
    /**
     * 删除充电站
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStation(@PathVariable UUID id) throws JCPPException {
        stationService.deleteStation(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
    
    /**
     * 获取充电站选项列表（用于下拉选择）
     */
    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<StationOption>>> getStationOptions() {
        List<StationOption> options = stationService.getStationOptions();
        return ResponseEntity.ok(ApiResponse.success("查询成功", options));
    }
    
    /**
     * 搜索充电站选项列表（支持关键字搜索和分页）
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StationOption>>> searchStationOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<StationOption> options = stationService.searchStationOptions(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("查询成功", options));
    }
    
    /**
     * 获取充电站-充电桩级联选择器数据（用于级联选择组件）
     */
    @GetMapping("/pile-cascader")
    public ResponseEntity<ApiResponse<List<StationPileCascaderOption>>> getStationPileCascaderOptions(
            @RequestParam(required = false) String keyword) {
        List<StationPileCascaderOption> options = stationService.getStationPileCascaderOptions(keyword);
        return ResponseEntity.ok(ApiResponse.success("查询成功", options));
    }
}
