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


/**
 * 仪表盘统计数据
 * 
 * @author 九筒
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    
    /**
     * 总览统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private Long totalStations;      // 总充电站数
        private Long totalPiles;         // 总充电桩数  
        private Long totalGuns;          // 总充电枪数
    }

    /**
     * 充电桩在线状态分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PileStatusDistribution {
        private Long onlinePiles;        // 在线充电桩数
        private Long offlinePiles;       // 离线充电桩数
        private Long totalPiles;         // 总充电桩数
        
        public double getOnlinePercentage() {
            return totalPiles > 0 ? (onlinePiles * 100.0) / totalPiles : 0.0;
        }
        
        public double getOfflinePercentage() {
            return totalPiles > 0 ? (offlinePiles * 100.0) / totalPiles : 0.0;
        }
    }

    /**
     * 充电枪运行状态分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GunStatusDistribution {
        private Long idleGuns;              // 空闲 (IDLE)
        private Long insertedGuns;          // 已插枪未充电 (INSERTED)
        private Long chargingGuns;          // 充电中 (CHARGING)
        private Long chargeCompleteGuns;    // 充电完成 (CHARGE_COMPLETE)
        private Long dischargeReadyGuns;    // 放电准备 (DISCHARGE_READY)
        private Long dischargingGuns;       // 放电中 (DISCHARGING)
        private Long dischargeCompleteGuns; // 放电完成 (DISCHARGE_COMPLETE)
        private Long reservedGuns;          // 预约 (RESERVED)
        private Long faultGuns;             // 故障 (FAULT)
        private Long totalGuns;             // 总充电枪数
        
        public double getIdlePercentage() {
            return totalGuns > 0 ? (idleGuns * 100.0) / totalGuns : 0.0;
        }
        
        public double getInsertedPercentage() {
            return totalGuns > 0 ? (insertedGuns * 100.0) / totalGuns : 0.0;
        }
        
        public double getChargingPercentage() {
            return totalGuns > 0 ? (chargingGuns * 100.0) / totalGuns : 0.0;
        }
        
        public double getChargeCompletePercentage() {
            return totalGuns > 0 ? (chargeCompleteGuns * 100.0) / totalGuns : 0.0;
        }
        
        public double getDischargeReadyPercentage() {
            return totalGuns > 0 ? (dischargeReadyGuns * 100.0) / totalGuns : 0.0;
        }
        
        public double getDischargingPercentage() {
            return totalGuns > 0 ? (dischargingGuns * 100.0) / totalGuns : 0.0;
        }
        
        public double getDischargeCompletePercentage() {
            return totalGuns > 0 ? (dischargeCompleteGuns * 100.0) / totalGuns : 0.0;
        }
        
        public double getReservedPercentage() {
            return totalGuns > 0 ? (reservedGuns * 100.0) / totalGuns : 0.0;
        }
        
        public double getFaultPercentage() {
            return totalGuns > 0 ? (faultGuns * 100.0) / totalGuns : 0.0;
        }
    }
    
    private Overview overview;
    private PileStatusDistribution pileStatusDistribution;
    private GunStatusDistribution gunStatusDistribution;
}
