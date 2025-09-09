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
import sanbing.jcpp.app.adapter.request.PileCreateRequest;
import sanbing.jcpp.app.adapter.request.PileQueryRequest;
import sanbing.jcpp.app.adapter.request.PileUpdateRequest;
import sanbing.jcpp.app.adapter.response.ApiResponse;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.adapter.response.PileOptionResponse;
import sanbing.jcpp.app.adapter.response.PileWithStatusResponse;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.exception.JCPPException;
import sanbing.jcpp.app.service.PileService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/piles")
@RequiredArgsConstructor
public class PileController extends BaseController {

    private final PileService pileService;

    @PostMapping
    public ResponseEntity<ApiResponse<Pile>> createPile(@Valid @RequestBody PileCreateRequest request) {
        Pile pile = pileService.createPile(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", pile));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Pile>> getPile(@PathVariable UUID id) {
        Pile pile = pileService.findById(id);
        if (pile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success("查询成功", pile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Pile>> updatePile(@PathVariable UUID id, 
                                                        @Valid @RequestBody PileUpdateRequest request) throws JCPPException {
        Pile pile = pileService.updatePile(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", pile));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePile(@PathVariable UUID id) throws JCPPException {
        pileService.deletePile(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PileWithStatusResponse>>> queryPilesWithStatus(PileQueryRequest request) {
        PageResponse<PileWithStatusResponse> piles = pileService.queryPilesWithStatus(request);
        return ResponseEntity.ok(ApiResponse.success("查询成功", piles));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<PileOptionResponse>>> getPileOptions() {
        List<PileOptionResponse> options = pileService.getPileOptions();
        return ResponseEntity.ok(ApiResponse.success("查询成功", options));
    }
}
