/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import {api} from './api';

// 协议选项接口
export interface ProtocolOption {
  value: string;       // 协议标识符
  label: string;       // 显示名称
}

/**
 * 获取所有支持的协议列表
 * @returns 协议选项列表
 */
export const getSupportedProtocols = async (): Promise<ProtocolOption[]> => {
  const response = await api.get('/api/protocols/supported');
  return response.data.data || [];
};
