/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */

import {message} from 'antd';

// 格式化时间戳为本地时间字符串 yyyy-MM-dd HH:mm:ss
export const formatTimestamp = (timestamp?: number): string => {
  if (!timestamp || timestamp <= 0) {
    return '-';
  }
  
  const date = new Date(timestamp);
  
  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return '-';
  }
  
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

// 生成充电桩编码（14位时间格式）
export const generatePileCode = (): string => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  const seconds = String(now.getSeconds()).padStart(2, '0');
  
  return `${year}${month}${day}${hours}${minutes}${seconds}`;
};

// 生成充电站编码（S+yyyyMMdd+三位随机数）
export const generateStationCode = (): string => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  
  // 生成三位随机数
  const randomNum = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
  
  return `S${year}${month}${day}${randomNum}`;
};

// 生成充电枪编码（充电桩编码+枪号）
export const generateGunCode = (pileCode: string, gunNo: string): string => {
  if (!pileCode || !gunNo) {
    return '';
  }
  return `${pileCode}-${gunNo}`;
};

// 获取状态颜色
export const getStatusColor = (status: string): string => {
  const colors: Record<string, string> = {
    'ONLINE': 'green',
    'OFFLINE': 'gray'
  };
  return colors[status] || 'default';
};

// 获取状态文本
export const getStatusText = (status: string): string => {
  const texts: Record<string, string> = {
    'ONLINE': '在线',
    'OFFLINE': '离线'
  };
  return texts[status] || status;
};

// 获取类型文本
export const getTypeText = (type: string): string => {
  return type === 'AC' ? '交流桩' : '直流桩';
};

// 获取协议文本
export const getProtocolText = (protocol: string): string => {
  const protocolTexts: Record<string, string> = {
    'yunkuaichongV150': '云快充V1.5.0',
    'yunkuaichongV160': '云快充V1.6.0',
    'yunkuaichongV170': '云快充V1.7.0',
    'lvnengV340': '绿能V3.4.0'
  };
  return protocolTexts[protocol] || protocol;
};

// 防抖函数
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void => {
  let timeoutId: NodeJS.Timeout;
  return (...args: Parameters<T>) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func.apply(null, args), delay);
  };
};

// 获取错误信息
export const getErrorMessage = (error: any): string => {
  if (error.response) {
    const { status, data } = error.response;
    
    // 根据HTTP状态码返回相应的错误信息
    switch (status) {
      case 400:
        return data?.message || '请求参数错误，请检查输入信息';
      case 401:
        return '未授权，请重新登录';
      case 403:
        return '权限不足，无法执行此操作';
      case 404:
        return '请求的资源不存在';
      case 409:
        return data?.message || '数据冲突，请刷新后重试';
      case 500:
        return '服务器内部错误，请稍后重试';
      default:
        return data?.message || `操作失败（状态码: ${status}）`;
    }
  } else if (error.request) {
    return '网络错误，请检查网络连接';
  } else {
    return error.message || '未知错误，请重试';
  }
};

// Message工具函数 - 根据Ant Design官方推荐的方式
export const showMessage = {
  success: (content: string) => {
    message.success({
      content,
      duration: 3, // 成功消息3秒
    });
  },
  
  error: (content: string) => {
    message.error({
      content,
      duration: 10, // 错误消息10秒
    });
  },
  
  warning: (content: string) => {
    message.warning({
      content,
      duration: 10, // 警告消息10秒
    });
  },
  
  info: (content: string) => {
    message.info({
      content,
      duration: 3, // 信息消息3秒
    });
  },
  
  loading: (content: string) => {
    return message.loading({
      content,
      duration: 0, // 加载消息不自动关闭
    });
  }
};
