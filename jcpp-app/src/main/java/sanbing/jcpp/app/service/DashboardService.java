/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service;

import sanbing.jcpp.app.adapter.response.DashboardStats;

/**
 * 仪表盘服务接口
 * 
 * @author 九筒
 */
public interface DashboardService {
    
    /**
     * 获取仪表盘统计数据
     */
    DashboardStats getDashboardStats();
}
