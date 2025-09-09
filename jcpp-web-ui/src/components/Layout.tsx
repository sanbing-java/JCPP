/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {useState} from 'react';
import {Avatar, Button, Dropdown, Layout as AntLayout, Menu, message} from 'antd';
import {
    AimOutlined,
    DashboardOutlined,
    DownOutlined,
    EnvironmentOutlined,
    LogoutOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    ThunderboltOutlined,
    UserOutlined
} from '@ant-design/icons';
import {useLocation, useNavigate} from 'react-router-dom';
import {useAuth} from '../contexts/AuthContext';

const { Header, Sider, Content } = AntLayout;

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();

  // 菜单项配置
  const menuItems = [
    {
      key: '/page/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: '/page/stations',
      icon: <EnvironmentOutlined />,
      label: '充电站管理',
    },
    {
      key: '/page/piles',
      icon: <ThunderboltOutlined />,
      label: '充电桩管理',
    },
    {
      key: '/page/guns',
      icon: <AimOutlined />,
      label: '充电枪管理',
    },
  ];

  // 处理菜单点击
  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key);
  };

  // 处理退出登录
  const handleLogout = () => {
    logout();
    message.success('已退出登录');
    navigate('/login');
  };

  // 用户下拉菜单
  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  // 获取当前选中的菜单项
  const getSelectedKeys = () => {
    return [location.pathname];
  };

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={220}
        style={{
          boxShadow: '2px 0 6px rgba(0, 21, 41, 0.35)',
        }}
      >
        <div style={{
          height: 64,
          lineHeight: '64px',
          textAlign: 'center',
          color: 'white',
          fontSize: 16,
          fontWeight: 600,
          background: 'rgba(255, 255, 255, 0.1)',
          marginBottom: 1,
        }}>
          {collapsed ? 'JCPP' : 'JCPP管理系统'}
        </div>
        
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={getSelectedKeys()}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>

      <AntLayout>
        <Header style={{
          background: '#fff',
          padding: '0 24px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)',
        }}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{
              fontSize: '16px',
              width: 64,
              height: 64,
            }}
          />

          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div className="user-info-wrapper">
              <Avatar 
                size={36}
                icon={<UserOutlined />} 
                className="user-avatar"
              />
              <div className="user-details">
                <span className="user-name">
                  {user?.username || '用户'}
                </span>
                <span className="user-role">管理员</span>
              </div>
              <DownOutlined className="dropdown-arrow" />
            </div>
          </Dropdown>
        </Header>

        <Content style={{
          margin: 24,
          padding: 24,
          background: '#fff',
          borderRadius: 6,
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
        }}>
          {children}
        </Content>
      </AntLayout>
    </AntLayout>
  );
};

export default Layout;