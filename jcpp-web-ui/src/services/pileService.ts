/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import api from './api';
import {
    ApiResponse,
    PageResponse,
    Pile,
    PileCreateRequest,
    PileOption,
    PileQueryRequest,
    PileUpdateRequest
} from '../types';

// 增强的API响应类型，包含HTTP状态码
export interface EnhancedApiResponse<T> extends ApiResponse<T> {
  httpStatus: number;
}

// 充电桩相关API
export const pileService = {
  // 分页查询充电桩
  async getPiles(params: PileQueryRequest): Promise<ApiResponse<PageResponse<Pile>>> {
    console.log('🔍 前端发送的查询参数:', params);  // 添加调试日志
    const response = await api.get('/api/piles', { params });
    console.log('📡 后端返回的响应:', response.data);  // 添加调试日志
    return response.data;
  },

  // 根据ID获取充电桩详情
  async getPile(id: string): Promise<ApiResponse<Pile>> {
    const response = await api.get(`/api/piles/${id}`);
    return response.data;
  },

  // 创建充电桩
  async createPile(data: PileCreateRequest): Promise<EnhancedApiResponse<Pile>> {
    const response = await api.post('/api/piles', data);
    return {
      ...response.data,
      httpStatus: response.status
    };
  },

  // 更新充电桩
  async updatePile(id: string, data: PileUpdateRequest): Promise<EnhancedApiResponse<Pile>> {
    const response = await api.put(`/api/piles/${id}`, data);
    return {
      ...response.data,
      httpStatus: response.status
    };
  },

  // 删除充电桩
  async deletePile(id: string): Promise<EnhancedApiResponse<void>> {
    const response = await api.delete(`/api/piles/${id}`);
    return {
      ...response.data,
      httpStatus: response.status
    };
  },

  // 获取充电桩选项列表
  async getPileOptions(): Promise<ApiResponse<PileOption[]>> {
    const response = await api.get('/api/piles/options');
    return response.data;
  }
};
