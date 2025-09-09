/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React from 'react';
import {Navigate} from 'react-router-dom';
import {useAuth} from '../contexts/AuthContext';
import {Spin} from 'antd';

/**
 * 404页面重定向组件
 * 根据用户登录状态智能重定向：
 * - 已登录用户：重定向到仪表盘
 * - 未登录用户：重定向到登录页
 */
const NotFoundRedirect: React.FC = () => {
  const { isAuthenticated, loading } = useAuth();

  // 如果正在加载认证状态，显示加载动画
  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <Spin size="large" />
      </div>
    );
  }

  // 根据登录状态进行重定向
  if (isAuthenticated) {
    // 已登录用户重定向到仪表盘
    return <Navigate to="/page/dashboard" replace />;
  } else {
    // 未登录用户重定向到登录页
    return <Navigate to="/login" replace />;
  }
};

export default NotFoundRedirect;


