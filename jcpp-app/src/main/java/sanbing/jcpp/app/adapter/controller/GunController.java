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
import sanbing.jcpp.app.adapter.request.GunCreateRequest;
import sanbing.jcpp.app.adapter.request.GunQueryRequest;
import sanbing.jcpp.app.adapter.request.GunUpdateRequest;
import sanbing.jcpp.app.adapter.response.ApiResponse;
import sanbing.jcpp.app.adapter.response.GunWithStatusResponse;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.app.service.GunService;

import java.util.UUID;

@RestController
@RequestMapping("/api/guns")
@RequiredArgsConstructor
public class GunController extends BaseController {

    private final GunService gunService;

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
}
