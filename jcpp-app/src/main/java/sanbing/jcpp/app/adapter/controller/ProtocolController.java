/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sanbing.jcpp.app.adapter.response.ApiResponse;
import sanbing.jcpp.app.adapter.response.ProtocolOption;
import sanbing.jcpp.app.service.ProtocolService;

import java.util.List;

/**
 * 协议管理控制器
 * 
 * @author 九筒
 * @since 2024-12-22
 */
@RestController
@RequestMapping("/api/protocols")
@RequiredArgsConstructor
public class ProtocolController extends BaseController {
    
    private final ProtocolService protocolService;
    
    /**
     * 获取所有支持的协议列表
     */
    @GetMapping("/supported")
    public ResponseEntity<ApiResponse<List<ProtocolOption>>> getSupportedProtocols() {
        List<ProtocolOption> protocols = protocolService.getSupportedProtocols();
        return ResponseEntity.ok(ApiResponse.success("查询成功", protocols));
    }
}