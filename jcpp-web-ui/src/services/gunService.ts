/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import {api} from './api';
import type {Gun, GunCreateRequest, GunUpdateRequest, PageResponse, QueryParams} from '../types';

export const getGuns = async (params: QueryParams): Promise<PageResponse<Gun>> => {
  const response = await api.get('/api/guns', { params });
  return response.data.data;
};

export const createGun = async (data: GunCreateRequest): Promise<Gun> => {
  const response = await api.post('/api/guns', data);
  return response.data.data;
};

export const updateGun = async (id: string, data: GunUpdateRequest): Promise<Gun> => {
  const response = await api.put(`/api/guns/${id}`, data);
  return response.data.data;
};

export const deleteGun = async (id: string): Promise<void> => {
  await api.delete(`/api/guns/${id}`);
};

export const getGun = async (id: string): Promise<Gun> => {
  const response = await api.get(`/api/guns/${id}`);
  return response.data.data;
};

export const getGunByCode = async (gunCode: string): Promise<Gun> => {
    const response = await api.get(`/api/guns/code/${gunCode}`);
    return response.data.data;
};

