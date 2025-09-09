/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.routing;

import cn.hutool.core.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 通用命令路由器
 * <p>
 * 提供基于协议名+命令字的路由功能，支持多版本协议
 *
 * @param <T> 命令执行器类型
 * @author 九筒
 * @since 2025-08-25
 */
@Slf4j
public class ProtocolCommandRouter<T> {
    
    private final Map<String, T> executorMap = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param scanBaseClass 要扫描的包的代表类
     * @param executorFilter 执行器过滤器，判断类是否为目标执行器类型
     */
    public ProtocolCommandRouter(Class<?> scanBaseClass, Predicate<Class<?>> executorFilter) {
        initializeRoutes(scanBaseClass, executorFilter);
    }

    /**
     * 初始化路由表
     */
    private void initializeRoutes(Class<?> scanBaseClass, Predicate<Class<?>> executorFilter) {
        Set<Class<?>> cmdClasses = ClassUtil.scanPackageByAnnotation(
            ClassUtil.getPackage(scanBaseClass), 
            ProtocolCmd.class
        );
        
        cmdClasses.stream()
                .filter(executorFilter)
                .forEach(this::registerExecutor);
    }

    /**
     * 注册命令执行器
     */
    private void registerExecutor(Class<?> executorClass) {
        ProtocolCmd annotation = executorClass.getAnnotation(ProtocolCmd.class);
        if (annotation == null) {
            log.warn("类 {} 没有 @ProtocolCmd 注解", executorClass.getName());
            return;
        }

        int cmd = annotation.value();
        String[] protocolNames = annotation.protocolNames();

        try {
            @SuppressWarnings("unchecked")
            T executor = (T) executorClass.getDeclaredConstructor().newInstance();
            
            for (String protocolName : protocolNames) {
                String key = buildKey(protocolName, cmd);
                executorMap.put(key, executor);
                log.debug("注册命令执行器: {} -> {}", key, executorClass.getSimpleName());
            }
        } catch (InstantiationException | IllegalAccessException | 
                 InvocationTargetException | NoSuchMethodException e) {
            log.error("无法实例化命令执行器 {}: {}", executorClass.getName(), e.getMessage());
            throw new RuntimeException("实例化命令执行器失败: " + executorClass.getName(), e);
        }
    }

    /**
     * 获取命令执行器
     *
     * @param protocolName 协议名称
     * @param cmd 命令字
     * @return 命令执行器，如果未找到则返回 null
     */
    public T getExecutor(String protocolName, int cmd) {
        String key = buildKey(protocolName, cmd);
        return executorMap.get(key);
    }

    /**
     * 构建路由键
     */
    private String buildKey(String protocolName, int cmd) {
        return protocolName + ":" + cmd;
    }

}