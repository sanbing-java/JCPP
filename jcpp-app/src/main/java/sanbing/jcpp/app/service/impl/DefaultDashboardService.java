/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.adapter.response.DashboardStats;
import sanbing.jcpp.app.dal.config.ibatis.enums.GunRunStatusEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileStatusEnum;
import sanbing.jcpp.app.dal.mapper.GunMapper;
import sanbing.jcpp.app.dal.mapper.PileMapper;
import sanbing.jcpp.app.dal.mapper.StationMapper;
import sanbing.jcpp.app.data.kv.AttrKeyEnum;
import sanbing.jcpp.app.service.DashboardService;


/**
 * 仪表盘服务实现
 * 
 * @author 九筒
 */
@Service
@RequiredArgsConstructor
public class DefaultDashboardService implements DashboardService {
    
    private final StationMapper stationMapper;
    private final PileMapper pileMapper;
    private final GunMapper gunMapper;
    
    @Override
    public DashboardStats getDashboardStats() {
        // 获取总览统计
        DashboardStats.Overview overview = buildOverview();
        
        // 获取充电桩状态分布
        DashboardStats.PileStatusDistribution pileStatusDistribution = buildPileStatusDistribution();
        
        // 获取充电枪状态分布
        DashboardStats.GunStatusDistribution gunStatusDistribution = buildGunStatusDistribution();
        
        return DashboardStats.builder()
                .overview(overview)
                .pileStatusDistribution(pileStatusDistribution)
                .gunStatusDistribution(gunStatusDistribution)
                .build();
    }
    
    private DashboardStats.Overview buildOverview() {
        // 统计充电站数量
        Long totalStations = stationMapper.selectCount(null);
        
        // 统计充电桩数量
        Long totalPiles = pileMapper.selectCount(null);
        
        // 统计充电枪数量
        Long totalGuns = gunMapper.selectCount(null);
        
        return DashboardStats.Overview.builder()
                .totalStations(totalStations)
                .totalPiles(totalPiles)
                .totalGuns(totalGuns)
                .build();
    }
    
    private DashboardStats.PileStatusDistribution buildPileStatusDistribution() {
        // 统计充电桩总数量
        Long totalPiles = pileMapper.selectCount(null);
        
        // 从数据库查询真实的在线/离线状态分布，使用枚举值作为参数
        String statusKey = AttrKeyEnum.STATUS.getCode();
        Long onlinePiles = pileMapper.countOnlinePiles(statusKey, PileStatusEnum.ONLINE.getValue());
        Long offlinePiles = pileMapper.countOfflinePiles(statusKey, PileStatusEnum.OFFLINE.getValue());
        
        return DashboardStats.PileStatusDistribution.builder()
                .totalPiles(totalPiles)
                .onlinePiles(onlinePiles)
                .offlinePiles(offlinePiles)
                .build();
    }
    
    private DashboardStats.GunStatusDistribution buildGunStatusDistribution() {
        // 统计充电枪总数量
        Long totalGuns = gunMapper.selectCount(null);
        
        // 从数据库查询真实的运行状态分布，按照GunRunStatusEnum枚举统计，使用枚举值作为参数
        String statusKey = AttrKeyEnum.GUN_RUN_STATUS.getCode();
        Long idleGuns = gunMapper.countIdleGuns(statusKey, GunRunStatusEnum.IDLE.getValue());
        Long insertedGuns = gunMapper.countInsertedGuns(statusKey, GunRunStatusEnum.INSERTED.getValue());
        Long chargingGuns = gunMapper.countChargingGuns(statusKey, GunRunStatusEnum.CHARGING.getValue());
        Long chargeCompleteGuns = gunMapper.countChargeCompleteGuns(statusKey, GunRunStatusEnum.CHARGE_COMPLETE.getValue());
        Long dischargeReadyGuns = gunMapper.countDischargeReadyGuns(statusKey, GunRunStatusEnum.DISCHARGE_READY.getValue());
        Long dischargingGuns = gunMapper.countDischargingGuns(statusKey, GunRunStatusEnum.DISCHARGING.getValue());
        Long dischargeCompleteGuns = gunMapper.countDischargeCompleteGuns(statusKey, GunRunStatusEnum.DISCHARGE_COMPLETE.getValue());
        Long reservedGuns = gunMapper.countReservedGuns(statusKey, GunRunStatusEnum.RESERVED.getValue());
        Long faultGuns = gunMapper.countFaultGuns(statusKey, GunRunStatusEnum.FAULT.getValue());
        
        return DashboardStats.GunStatusDistribution.builder()
                .totalGuns(totalGuns)
                .idleGuns(idleGuns)
                .insertedGuns(insertedGuns)
                .chargingGuns(chargingGuns)
                .chargeCompleteGuns(chargeCompleteGuns)
                .dischargeReadyGuns(dischargeReadyGuns)
                .dischargingGuns(dischargingGuns)
                .dischargeCompleteGuns(dischargeCompleteGuns)
                .reservedGuns(reservedGuns)
                .faultGuns(faultGuns)
                .build();
    }
    
}
