/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.controller;

import com.google.common.util.concurrent.ListenableFuture;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sanbing.jcpp.app.adapter.request.GunCreateRequest;
import sanbing.jcpp.app.adapter.request.GunQueryRequest;
import sanbing.jcpp.app.adapter.request.GunUpdateRequest;
import sanbing.jcpp.app.adapter.response.ApiResponse;
import sanbing.jcpp.app.adapter.response.GunWithStatusResponse;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.app.data.kv.AttrKeyEnum;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.app.service.AttributeService;
import sanbing.jcpp.app.service.GunService;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/guns")
@RequiredArgsConstructor
@Slf4j
public class GunController extends BaseController {

    private final GunService gunService;
    private final AttributeService attributeService;

    @PostMapping
    public ResponseEntity<ApiResponse<Gun>> createGun(@Valid @RequestBody GunCreateRequest request) {
        Gun gun = gunService.createGun(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", gun));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Gun>> getGun(@PathVariable UUID id) {
        Gun gun = gunService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", gun));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Gun>> updateGun(@PathVariable UUID id, 
                                                      @Valid @RequestBody GunUpdateRequest request) {
        Gun gun = gunService.updateGun(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", gun));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGun(@PathVariable UUID id) {
        gunService.deleteGun(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GunWithStatusResponse>>> queryGunsWithStatus(GunQueryRequest request) {
        PageResponse<GunWithStatusResponse> guns = gunService.queryGunsWithStatus(request);
        return ResponseEntity.ok(ApiResponse.success("查询成功", guns));
    }

    /**
     * 根据枪编号获取充电枪运行状态
     */
    @GetMapping("/status/{gunCode}")
    public ResponseEntity<ApiResponse<String>> getGunStatusByGunCode(@PathVariable String gunCode) {
        try {
            // 首先根据枪编号查找充电枪
            Gun gun = gunService.findByGunCode(gunCode);
            if (gun == null) {
                return ResponseEntity.ok(ApiResponse.error("充电枪不存在", null));
            }

            // 通过AttributeService获取充电枪运行状态
            ListenableFuture<Optional<AttributeKvEntry>> attributeFuture =
                    attributeService.find(gun.getId(), AttrKeyEnum.GUN_RUN_STATUS.getCode());
            
            Optional<AttributeKvEntry> attributeResult = attributeFuture.get();
            String status = null;
            if (attributeResult.isPresent()) {
                AttributeKvEntry entry = attributeResult.get();
                status = entry.getStrValue().orElse(null);
            }

            return ResponseEntity.ok(ApiResponse.success("查询成功", status));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage(), null));
        }
    }

    /**
     * 根据充电枪编码获取充电枪详细信息
     */
    @GetMapping("/code/{gunCode}")
    public ResponseEntity<ApiResponse<GunWithStatusResponse>> getGunByCode(@PathVariable String gunCode) {
        try {
            GunWithStatusResponse gun = gunService.findGunWithStatusByCode(gunCode);
            if (gun == null) {
                return ResponseEntity.ok(ApiResponse.error("充电枪不存在", null));
            }
            return ResponseEntity.ok(ApiResponse.success("查询成功", gun));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage(), null));
        }
    }

}
