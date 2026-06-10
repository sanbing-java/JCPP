/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.app.service;

import sanbing.jcpp.app.adapter.response.ProtocolOption;

import java.util.List;

/**
 * 协议服务接口
 * 
 * @author 九筒
 * @since 2024-12-22
 */
public interface ProtocolService {
    
    /**
     * 获取所有支持的协议选项列表
     * @return 协议选项列表
     */
    List<ProtocolOption> getSupportedProtocols();

}
