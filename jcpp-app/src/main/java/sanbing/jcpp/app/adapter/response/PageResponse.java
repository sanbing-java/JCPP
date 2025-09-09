/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应
 * 
 * @author 九筒
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    
    private List<T> records;        // 数据列表
    private Long total;             // 总记录数
    private Integer page;           // 当前页码
    private Integer size;           // 每页大小
    private Integer totalPages;     // 总页数
    
    public static <T> PageResponse<T> of(List<T> records, Long total, Integer page, Integer size) {
        int totalPages = (int) Math.ceil((double) total / size);
        return PageResponse.<T>builder()
                .records(records)
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }
}
