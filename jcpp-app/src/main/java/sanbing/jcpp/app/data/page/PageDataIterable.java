/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.data.page;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 分页数据迭代器，用于处理大数据量查询，避免内存溢出
 * 
 * @param <T> 数据类型
 * @author 九筒
 */
public class PageDataIterable<T> implements Iterable<T> {
    
    private final FetchFunction<T> fetchFunction;
    private final int pageSize;
    
    public PageDataIterable(FetchFunction<T> fetchFunction, int pageSize) {
        this.fetchFunction = fetchFunction;
        this.pageSize = pageSize;
    }
    
    @Override
    public Iterator<T> iterator() {
        return new PageDataIterator();
    }
    
    /**
     * 分页获取函数接口
     */
    @FunctionalInterface
    public interface FetchFunction<T> {
        /**
         * 获取指定页的数据
         * 
         * @param offset 偏移量
         * @param limit 限制数量
         * @return 数据列表
         */
        List<T> fetch(int offset, int limit);
    }
    
    private class PageDataIterator implements Iterator<T> {
        private int currentOffset = 0;
        private List<T> currentPage;
        private int currentIndex = 0;
        private boolean hasMorePages = true;
        
        @Override
        public boolean hasNext() {
            // 如果当前页还有数据，直接返回true
            if (currentPage != null && currentIndex < currentPage.size()) {
                return true;
            }
            
            // 如果没有更多页了，返回false
            if (!hasMorePages) {
                return false;
            }
            
            // 尝试加载下一页
            loadNextPage();
            
            // 检查加载后是否有数据
            return currentPage != null && !currentPage.isEmpty();
        }
        
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            return currentPage.get(currentIndex++);
        }
        
        private void loadNextPage() {
            try {
                currentPage = fetchFunction.fetch(currentOffset, pageSize);
                currentIndex = 0;
                
                // 如果返回的数据少于页大小，说明没有更多页了
                if (currentPage == null || currentPage.size() < pageSize) {
                    hasMorePages = false;
                }
                
                // 更新偏移量
                currentOffset += pageSize;
                
            } catch (Exception e) {
                hasMorePages = false;
                currentPage = null;
                throw new RuntimeException("Failed to fetch next page", e);
            }
        }
    }
}
