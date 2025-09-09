/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import {api} from './api';

/**
 * 总览统计
 */
export interface Overview {
  totalStations: number;      // 总充电站数
  totalPiles: number;         // 总充电桩数  
  totalGuns: number;          // 总充电枪数
}

/**
 * 充电桩在线状态分布
 */
export interface PileStatusDistribution {
  onlinePiles: number;        // 在线充电桩数
  offlinePiles: number;       // 离线充电桩数
  totalPiles: number;         // 总充电桩数
  onlinePercentage: number;   // 在线百分比
  offlinePercentage: number;  // 离线百分比
}

/**
 * 充电枪运行状态分布
 */
export interface GunStatusDistribution {
  idleGuns: number;              // 空闲 (IDLE)
  insertedGuns: number;          // 已插枪未充电 (INSERTED)
  chargingGuns: number;          // 充电中 (CHARGING)
  chargeCompleteGuns: number;    // 充电完成 (CHARGE_COMPLETE)
  dischargeReadyGuns: number;    // 放电准备 (DISCHARGE_READY)
  dischargingGuns: number;       // 放电中 (DISCHARGING)
  dischargeCompleteGuns: number; // 放电完成 (DISCHARGE_COMPLETE)
  reservedGuns: number;          // 预约 (RESERVED)
  faultGuns: number;             // 故障 (FAULT)
  totalGuns: number;             // 总充电枪数
  idlePercentage: number;        // 空闲百分比
  insertedPercentage: number;    // 已插枪百分比
  chargingPercentage: number;    // 充电中百分比
  chargeCompletePercentage: number;     // 充电完成百分比
  dischargeReadyPercentage: number;     // 放电准备百分比
  dischargingPercentage: number;        // 放电中百分比
  dischargeCompletePercentage: number;  // 放电完成百分比
  reservedPercentage: number;           // 预约百分比
  faultPercentage: number;              // 故障百分比
}

/**
 * 仪表盘统计数据
 */
export interface DashboardStats {
  overview: Overview;
  pileStatusDistribution: PileStatusDistribution;
  gunStatusDistribution: GunStatusDistribution;
}

/**
 * 获取仪表盘统计数据
 */
export const getDashboardStats = async (): Promise<DashboardStats> => {
  const response = await api.get('/api/dashboard/stats');
  return response.data.data;
};
