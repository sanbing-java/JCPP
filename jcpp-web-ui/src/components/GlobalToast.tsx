/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {useEffect, useState} from 'react';
import {Alert} from 'antd';
import {CheckCircleOutlined, CloseCircleOutlined} from '@ant-design/icons';

export interface ToastMessage {
  id: string;
  message: string;
  type: 'success' | 'error';
  duration?: number; // 显示时长，毫秒
}

interface GlobalToastProps {
  messages: ToastMessage[];
  onRemove: (id: string) => void;
}

const GlobalToast: React.FC<GlobalToastProps> = ({ messages, onRemove }) => {
  const [visibleMessages, setVisibleMessages] = useState<ToastMessage[]>([]);

  useEffect(() => {
    setVisibleMessages(messages);

    // 为每个消息设置自动消失定时器
    messages.forEach((message) => {
      const duration = message.duration || 3000; // 默认3秒
      setTimeout(() => {
        onRemove(message.id);
      }, duration);
    });
  }, [messages, onRemove]);

  if (visibleMessages.length === 0) {
    return null;
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: 20,
        right: 20,
        zIndex: 9999,
        maxWidth: '400px',
        pointerEvents: 'none'
      }}
    >
      {visibleMessages.map((message, index) => (
        <div
          key={message.id}
          style={{
            marginBottom: '12px',
            pointerEvents: 'auto',
            animation: 'slideInRight 0.3s ease-out',
            transition: 'all 0.3s ease-in-out'
          }}
        >
          <Alert
            message={message.message}
            type={message.type}
            icon={message.type === 'success' ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
            showIcon
            closable
            onClose={() => onRemove(message.id)}
            style={{
              borderRadius: '12px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.12)',
              border: 'none',
              backgroundColor: message.type === 'success' ? '#f6ffed' : '#fff2f0',
              fontSize: '14px',
              lineHeight: '1.5',
              padding: '12px 16px',
              minHeight: '56px',
              display: 'flex',
              alignItems: 'center',
              backdropFilter: 'blur(8px)',
              ...(message.type === 'success' ? {
                background: 'linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%)',
                borderLeft: '4px solid #52c41a'
              } : {
                background: 'linear-gradient(135deg, #fff2f0 0%, #ffccc7 100%)',
                borderLeft: '4px solid #ff4d4f'
              })
            }}
          />
        </div>
      ))}
      <style>
        {`
          @keyframes slideInRight {
            from {
              transform: translateX(100%);
              opacity: 0;
            }
            to {
              transform: translateX(0);
              opacity: 1;
            }
          }
        `}
      </style>
    </div>
  );
};

export default GlobalToast;
