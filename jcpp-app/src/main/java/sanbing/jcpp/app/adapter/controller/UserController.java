/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sanbing.jcpp.app.adapter.response.ApiResponse;
import sanbing.jcpp.app.adapter.response.ErrorCode;
import sanbing.jcpp.app.adapter.response.LoginResponse;
import sanbing.jcpp.app.service.security.model.SecurityUser;

/**
 * 用户控制器
 * 
 * @author 九筒
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController extends BaseController {
    
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
                return ResponseEntity.status(401).body(ApiResponse.error(ErrorCode.UNAUTHORIZED));
            }

            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                    .id(securityUser.getId().toString())
                    .username(securityUser.getUserName())
                    .status(securityUser.isEnabled() ? "ENABLE" : "DISABLE")
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(userInfo));
            
        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            return ResponseEntity.status(500).body(ApiResponse.error("获取用户信息失败"));
        }
    }
}
