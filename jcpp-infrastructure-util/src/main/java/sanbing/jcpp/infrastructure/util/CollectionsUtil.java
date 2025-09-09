/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionsUtil {
    /**
     * 判断集合是否为空（null 或者 size 为 0）。
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为空。
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 返回存在于集合 B（新）但不在集合 A（旧）中的元素的新集合。
     */
    public static <T> Set<T> diffSets(Set<T> a, Set<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toSet());
    }

    /**
     * 返回存在于列表 B（新）但不在列表 A（旧）中的元素的新列表。
     */
    public static <T> List<T> diffLists(List<T> a, List<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toList());
    }

    /**
     * 判断集合中是否包含指定元素。
     */
    public static <T> boolean contains(Collection<T> collection, T element) {
        return isNotEmpty(collection) && collection.contains(element);
    }

    /**
     * 统计数组中非空元素的数量。
     */
    public static <T> int countNonNull(T[] array) {
        int count = 0;
        for (T t : array) {
            if (t != null) count++;
        }
        return count;
    }

    /**
     * 创建一个 Map，传入键值对参数。
     * 如果参数数量不是偶数，则抛出异常。
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<T, T> mapOf(T... kvs) {
        if (kvs.length % 2 != 0) {
            throw new IllegalArgumentException("参数数量无效");
        }
        Map<T, T> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            T key = kvs[i];
            T value = kvs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    /**
     * 判断集合是否为空或者包含指定元素。
     */
    public static <V> boolean emptyOrContains(Collection<V> collection, V element) {
        return isEmpty(collection) || collection.contains(element);
    }

    /**
     * 合并两个集合并返回一个新的 HashSet。
     */
    public static <V> HashSet<V> concat(Set<V> set1, Set<V> set2) {
        HashSet<V> result = new HashSet<>();
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

}
