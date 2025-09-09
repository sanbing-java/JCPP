/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import {api} from './api';
import type {PageResponse, QueryParams, Station, StationOption, StationSearchRequest} from '../types';

export const getStations = async (params: QueryParams): Promise<PageResponse<Station>> => {
  const response = await api.get('/api/stations', { params });
  return response.data.data;
};

export const createStation = async (data: Partial<Station>): Promise<Station> => {
  const response = await api.post('/api/stations', data);
  return response.data.data;
};

export const updateStation = async (id: string, data: Partial<Station>): Promise<Station> => {
  const response = await api.put(`/api/stations/${id}`, data);
  return response.data.data;
};

export const deleteStation = async (id: string): Promise<void> => {
  await api.delete(`/api/stations/${id}`);
};

export const getStation = async (id: string): Promise<Station> => {
  const response = await api.get(`/api/stations/${id}`);
  return response.data.data;
};

export const searchStationOptions = async (params: StationSearchRequest): Promise<StationOption[]> => {
  const response = await api.get('/api/stations/search', { params });
  return response.data.data;
};

export const getStationOptions = async (): Promise<StationOption[]> => {
  const response = await api.get('/api/stations/options');
  return response.data.data;
};