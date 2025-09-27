/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */

// API响应通用格式
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

// 分页响应
export interface PageResponse<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

// 分页请求参数
export interface PageRequest {
  page: number;
  size: number;
  sortField?: string;    // 修改为与后端一致
  sortOrder?: 'asc' | 'desc';  // 修改为与后端一致的小写
  search?: string;       // 添加后端的search字段
}

// 充电桩相关类型
export interface Pile {
  id: string;
  pileName: string;
  pileCode: string;
  protocol: string;
  type: 'AC' | 'DC';
  brand: string;
  model: string;
  manufacturer: string;
  stationId: string;
  status: 'IDLE' | 'CHARGING' | 'FAULT' | 'OFFLINE';
  connectedAt?: number;
  disconnectedAt?: number;
  lastActiveTime?: number;
    gunCount?: number;
  createdTime: number;
  updatedTime?: number;
}

// 充电桩创建请求
export interface PileCreateRequest {
  pileName: string;
  pileCode: string;
  protocol: string;
  type: 'AC' | 'DC';
  brand: string;
  model: string;
  manufacturer: string;
  stationId: string;
}

// 充电桩更新请求
export interface PileUpdateRequest {
  pileName: string;
  protocol: string;
  type: 'AC' | 'DC';
  brand: string;
  model: string;
  manufacturer: string;
  stationId: string;
}

// 充电桩查询请求
export interface PileQueryRequest extends PageRequest {
  pileName?: string;
  pileCode?: string;
  protocol?: string;     // 添加协议字段
  stationId?: string;    // 添加充电站ID字段
  brand?: string;
  model?: string;        // 添加型号字段
  manufacturer?: string;
  type?: 'AC' | 'DC';
  status?: 'ONLINE' | 'OFFLINE';  // 修改状态字段
}

// 充电站相关类型
export interface Station {
  id: string;
  stationName: string;
  stationCode: string;
  longitude?: number;
  latitude?: number;
  province?: string;
  city?: string;
  county?: string;
  address?: string;
  createdTime: number;
  updatedTime?: number;
}

// 充电站选项
export interface StationOption {
  id: string;
  label: string;
  stationName: string;
  stationCode: string;
}

// 充电站搜索请求
export interface StationSearchRequest {
  keyword?: string;
  page?: number;
  size?: number;
}

// 充电枪相关类型
export interface Gun {
  id: string;
  gunName: string;
  gunCode: string;
  gunNo: string;
  stationId: string;
  stationName?: string;  // 所属充电站名称
  pileId: string;
  pileCode: string;
  pileName?: string;     // 所属充电桩名称
  runStatus: 'IDLE' | 'INSERTED' | 'CHARGING' | 'CHARGE_COMPLETE' | 'DISCHARGE_READY' | 'DISCHARGING' | 'DISCHARGE_COMPLETE' | 'RESERVED' | 'FAULT';
  createdTime: number;
  updatedTime?: number;
}

// 充电枪创建请求
export interface GunCreateRequest {
  gunName: string;
  gunNo: string;
  gunCode: string;
  stationId: string;
  pileId: string;
}

// 充电枪更新请求
export interface GunUpdateRequest {
  gunName: string;
  gunNo: string;
  gunCode: string;
  stationId: string;
  pileId: string;
}

// 充电桩选项
export interface PileOption {
  id: string;
  label: string;
  pileName: string;
  pileCode: string;
  stationId: string;
}

// 仪表盘统计数据
export interface DashboardStats {
  overview: {
    totalStations: number;
    totalPiles: number;
    totalGuns: number;
    onlineGuns: number;
    offlineGuns: number;
    idleGuns: number;
    chargingGuns: number;
    faultGuns: number;
  };
  recentActivities: Array<{
    id: string;
    title: string;
    description: string;
    timestamp: number;
    type: 'INFO' | 'WARNING' | 'ERROR';
  }>;
}

// 通用查询参数
export interface QueryParams {
  page?: number;
  size?: number;
  keyword?: string;
}

// 分页组件参数
export interface Pagination {
  current: number;
  pageSize: number;
  total: number;
}

// 充电站创建和更新请求
export interface StationCreateRequest {
  stationName: string;
  stationCode: string;
  longitude: number;
  latitude: number;
  province: string;
  city: string;
  county?: string;
  address: string;
}

export interface StationUpdateRequest extends StationCreateRequest {
}

// 充电站查询请求
export interface StationQueryRequest extends PageRequest {
  stationName?: string;
  stationCode?: string;
  province?: string;
  city?: string;
  county?: string;
  address?: string;
}

// 充电枪查询请求
export interface GunQueryRequest extends PageRequest {
  gunName?: string;
  gunCode?: string;
  gunNo?: string;
  stationId?: string;
  pileId?: string;
  runStatus?: 'IDLE' | 'INSERTED' | 'CHARGING' | 'CHARGE_COMPLETE' | 'DISCHARGE_READY' | 'DISCHARGING' | 'DISCHARGE_COMPLETE' | 'RESERVED' | 'FAULT';
}
