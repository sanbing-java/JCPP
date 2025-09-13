/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import axios, {AxiosError, AxiosResponse} from 'axios';
import {message} from 'antd';

// 获取API基础URL的函数
const getApiBaseUrl = (): string => {
  // 如果设置了环境变量，优先使用环境变量
  if (process.env.REACT_APP_API_BASE_URL) {
    return process.env.REACT_APP_API_BASE_URL;
  }
  
  // 根据构建环境决定API基础URL
  const env = process.env.REACT_APP_ENV || 'development';
  
  if (env === 'production') {
    // 生产环境：使用当前页面的协议和域名
    const protocol = window.location.protocol;
    const host = window.location.host;
    return `${protocol}//${host}`;
  } else {
    // 开发环境：使用localhost:8080
    return 'http://localhost:8080';
  }
};

// 创建axios实例
const api = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json;charset=UTF-8'
  }
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    console.log('Request interceptor - Token:', token ? 'exists' : 'missing'); // 调试日志
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('Authorization header set:', config.headers.Authorization); // 调试日志
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 根据HTTP状态码返回友好的错误提示
const getErrorMessage = (error: AxiosError): string => {
  if (!error.response) {
    return '网络连接失败，请检查网络连接';
  }
  
  const status = error.response.status;
  const data = error.response.data as any;
  
  // 优先使用ApiResponse格式的错误信息
  const apiErrorMessage = data?.message;
  const errorCode = data?.errorCode;
  
  // 根据错误码提供特殊处理
  if (errorCode) {
    switch (errorCode) {
      // 通用错误码
      case 'BUSINESS_ERROR':
        return apiErrorMessage || '业务处理失败';
      case 'SYSTEM_ERROR':
        return apiErrorMessage || '系统错误';
      
      // 参数校验相关
      case 'VALIDATION_ERROR':
        return apiErrorMessage || '数据验证失败';
      case 'BINDING_ERROR':
        return apiErrorMessage || '数据绑定失败';
      case 'ILLEGAL_ARGUMENT':
        return apiErrorMessage || '参数错误';
      case 'ILLEGAL_STATE':
        return apiErrorMessage || '状态错误';
      
      // 认证授权相关
      case 'UNAUTHORIZED':
        return apiErrorMessage || '用户未认证';
      case 'AUTH_FAILED':
        return apiErrorMessage || '用户名或密码错误';
      case 'JWT_AUTH_FAILED':
        return apiErrorMessage || 'Token认证失败';
      case 'FORBIDDEN':
        return apiErrorMessage || '权限不足';
      
      // 资源相关
      case 'NOT_FOUND':
        return apiErrorMessage || '资源不存在';
      case 'CONFLICT':
        return apiErrorMessage || '资源冲突';
      
      // 业务特定错误码
      case 'PILE_CODE_EXISTS':
        return apiErrorMessage || '充电桩编码已存在';
      case 'STATION_NAME_EXISTS':
        return apiErrorMessage || '充电站名称已存在';
      case 'GUN_CODE_EXISTS':
        return apiErrorMessage || '充电枪编号已存在';
      case 'PILE_NOT_FOUND':
        return apiErrorMessage || '充电桩不存在';
      case 'STATION_NOT_FOUND':
        return apiErrorMessage || '充电站不存在';
      case 'GUN_NOT_FOUND':
        return apiErrorMessage || '充电枪不存在';
      
      default:
        // 对于未知错误码，继续使用消息内容
        break;
    }
  }
  
  // 根据HTTP状态码提供后备错误信息
  switch (status) {
    case 400:
      return apiErrorMessage || '请求参数错误，请检查输入信息';
    case 401:
      return apiErrorMessage || '未授权，请重新登录';
    case 403:
      return apiErrorMessage || '没有权限执行此操作';
    case 404:
      return apiErrorMessage || '请求的资源不存在';
    case 409:
      return apiErrorMessage || '数据冲突，请刷新后重试';
    case 422:
      return apiErrorMessage || '数据验证失败，请检查输入信息';
    case 500:
      return apiErrorMessage || '服务器内部错误，请稍后重试';
    case 502:
      return '服务器网关错误，请稍后重试';
    case 503:
      return '服务不可用，请稍后重试';
    case 504:
      return '服务器响应超时，请稍后重试';
    default:
      return apiErrorMessage || `操作失败（状态码: ${status}）`;
  }
};

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error: AxiosError) => {
    if (error.response) {
      const { status } = error.response;
      const config = error.config;
      const data = error.response.data as any;
      const apiErrorMessage = data?.message;
      // const errorCode = data?.errorCode; // 暂时不在拦截器中使用errorCode
      
      // 如果是登录接口的401错误，不进行全局处理，让组件自己处理
      if (status === 401 && config?.url?.includes('/api/auth/login')) {
        return Promise.reject(error);
      }
      
      switch (status) {
        case 401:
          message.error(apiErrorMessage || '未授权，请重新登录');
          localStorage.removeItem('token');
          window.location.href = '/login';
          break;
        case 403:
          message.error(apiErrorMessage || '没有权限访问');
          break;
        case 404:
          message.error(apiErrorMessage || '请求的资源不存在');
          break;
        default:
          // 对于其他错误（包括500），不在拦截器中显示消息，让组件自己处理
          break;
      }
    } else if (error.request) {
      message.error('网络错误，请检查网络连接');
    } else {
      message.error('请求配置错误');
    }
    
    return Promise.reject(error);
  }
);

export { getErrorMessage, api };
export default api;
