/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.request;

import lombok.Data;

/**
 * 分页查询请求基类
 * 
 * @author 九筒
 */
@Data
public class PageRequest {
    
    private Integer page = 1;           // 页码，从1开始
    private Integer size = 10;          // 每页大小
    private String sortField;           // 排序字段
    private String sortOrder = "desc";  // 排序方向：asc, desc
    private String search;              // 搜索关键词
    
    /**
     * 获取MyBatis-Plus的页码（从0开始）
     */
    public long getOffset() {
        return (long) (page - 1) * size;
    }
    
    /**
     * 兼容方法：获取排序字段
     */
    public String getSortBy() {
        return sortField;
    }
}
