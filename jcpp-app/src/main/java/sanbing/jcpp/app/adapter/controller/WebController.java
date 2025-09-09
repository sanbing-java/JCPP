/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA（单页应用）路由控制器
 * 处理所有前端路由，返回index.html
 *
 * @author 九筒
 */
@Controller
public class WebController extends BaseController {

    /**
     * 处理所有业务页面路由
     * 统一使用 /page/ 前缀，便于扩展管理
     */
    @GetMapping(value = {"/", "/login", "/page/**"})
    public String redirect() {
        return "forward:/index.html";
    }
}
