/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.config.ibatis.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 充电桩状态枚举 - 简化版本，只维护在线/离线状态
 * <p>
 * 设计原则：
 * - 充电桩状态独立于充电枪状态，不受枪的工作状态影响
 * - 只关注设备的网络连接状态和基础可用性
 * - 充电枪的具体工作状态通过GunRunStatusEnum单独维护
 * <p>
 * 状态转换场景：
 * 1. 设备登录成功 → ONLINE
 * 2. 设备心跳正常 → 保持ONLINE
 * 3. 设备断开连接 → OFFLINE
 * 4. 设备超时无响应 → OFFLINE
 * 5. 系统重启后清洗 → 根据连接状态决定ONLINE/OFFLINE
 * 
 * @author 九筒
 */
public enum PileStatusEnum implements IEnum<String> {
    /**
     * 在线状态：设备已连接并能正常通信
     * - 设备登录成功
     * - 心跳正常
     * - 能接收和响应指令
     */
    ONLINE,
    
    /**
     * 离线状态：设备未连接或无法通信
     * - 设备未登录
     * - 网络连接断开
     * - 心跳超时
     * - 系统重启后未重新连接
     */
    OFFLINE,
    ;

    @Override
    public String getValue() {
        return name();
    }
}