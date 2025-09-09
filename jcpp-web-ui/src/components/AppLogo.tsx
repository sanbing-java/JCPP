/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React from 'react';
import logo192 from '../assets/icons/logo192.svg';

interface AppLogoProps {
  size?: number;
  className?: string;
}

/**
 * 应用Logo组件
 * 使用新的麻将桌风格充电桩图标
 */
const AppLogo: React.FC<AppLogoProps> = ({ size = 48, className }) => {
  return (
    <img 
      src={logo192} 
      alt="JCPP充电桩管理系统" 
      width={size} 
      height={size}
      className={className}
      style={{
        borderRadius: '8px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
      }}
    />
  );
};

export default AppLogo;
