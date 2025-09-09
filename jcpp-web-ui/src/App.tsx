/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {useEffect} from 'react';
import {BrowserRouter as Router, Navigate, Route, Routes} from 'react-router-dom';
import {ConfigProvider, message} from 'antd';
import zhCN from 'antd/locale/zh_CN';
import {AuthProvider} from './contexts/AuthContext';
import {ToastProvider} from './contexts/ToastContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import StationManagement from './components/StationManagement';
import PileManagement from './components/PileManagement';
import GunManagement from './components/GunManagement';
import NotFoundRedirect from './components/NotFoundRedirect';
import './App.css';

const App: React.FC = () => {
  // 配置全局message
  useEffect(() => {
    message.config({
      top: 50,              // 距离顶部位置
      duration: 3,          // 默认持续时间3秒
      maxCount: 3,          // 最多同时显示3个
    });
  }, []);

  return (
    <ConfigProvider locale={zhCN}>
      <AuthProvider>
        <ToastProvider>
          <Router>
          <Routes>
            {/* 登录页面 */}
            <Route path="/login" element={<Login />} />
            
            {/* 根路径重定向 */}
            <Route path="/" element={<Navigate to="/page/dashboard" replace />} />
            
            {/* 受保护的路由 - 使用 /page 前缀 */}
            <Route path="/page/dashboard" element={
              <ProtectedRoute>
                <Layout>
                  <Dashboard />
                </Layout>
              </ProtectedRoute>
            } />
            <Route path="/page/stations" element={
              <ProtectedRoute>
                <Layout>
                  <StationManagement />
                </Layout>
              </ProtectedRoute>
            } />
            <Route path="/page/piles" element={
              <ProtectedRoute>
                <Layout>
                  <PileManagement />
                </Layout>
              </ProtectedRoute>
            } />
            <Route path="/page/guns" element={
              <ProtectedRoute>
                <Layout>
                  <GunManagement />
                </Layout>
              </ProtectedRoute>
            } />
            
            {/* 智能404重定向 - 根据登录状态决定重定向目标 */}
            <Route path="*" element={<NotFoundRedirect />} />
          </Routes>
          </Router>
        </ToastProvider>
      </AuthProvider>
    </ConfigProvider>
  );
};

export default App;