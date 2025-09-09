/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {createContext, ReactNode, useContext, useEffect, useState} from 'react';
import {message} from 'antd';
import {api} from '../services/api';

interface User {
  id: string;
  username: string;
  email?: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (credentials: { username: string; password: string }) => Promise<boolean>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  const isAuthenticated = !!token && !!user;

  // 初始化认证状态
  useEffect(() => {
    const initAuth = async () => {
      const savedToken = localStorage.getItem('token');
      if (savedToken) {
        setToken(savedToken);
        try {
          await fetchUserInfo();
        } catch (error) {
          console.error('获取用户信息失败:', error);
          logout();
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const fetchUserInfo = async () => {
    try {
      const response = await api.get('/api/user/info');
      // 适配后端API响应格式：ApiResponse<UserInfo>
      const userData = response.data?.data || response.data;
      setUser({
        id: userData.id,
        username: userData.username,
        email: userData.email
      });
    } catch (error) {
      throw error;
    }
  };

  const login = async (credentials: { username: string; password: string }): Promise<boolean> => {
    try {
      const response = await api.post('/api/auth/login', credentials);
      console.log('Login response:', response.data); // 调试日志
      
      // 检查响应数据结构
      const responseData = response.data;
      let newToken: string;
      let userInfo: User;

      // 适配不同的响应格式
      if (responseData.token) {
        newToken = responseData.token;
        userInfo = responseData.user || { id: 'unknown', username: credentials.username };
      } else if (responseData.data?.token) {
        newToken = responseData.data.token;
        userInfo = responseData.data.user || { id: 'unknown', username: credentials.username };
      } else {
        // 如果没有token字段，假设整个response就是token
        newToken = responseData;
        userInfo = { id: 'unknown', username: credentials.username };
      }
      
      console.log('Extracted token:', newToken);
      console.log('Extracted user:', userInfo);
      
      setToken(newToken);
      setUser(userInfo);
      localStorage.setItem('token', newToken);
      
      return true;
    } catch (error: any) {
      console.error('登录失败:', error);
      message.error(error.response?.data?.message || '登录失败');
      return false;
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('token');
  };

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated,
    login,
    logout,
    loading
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
