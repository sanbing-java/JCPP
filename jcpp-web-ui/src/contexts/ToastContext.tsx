/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {createContext, ReactNode, useContext, useState} from 'react';
import GlobalToast, {ToastMessage} from '../components/GlobalToast';

interface ToastContextType {
  showToast: (message: string, httpStatus?: number, duration?: number) => void;
  showSuccess: (message: string, duration?: number) => void;
  showError: (message: string, duration?: number) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};

interface ToastProviderProps {
  children: ReactNode;
}

export const ToastProvider: React.FC<ToastProviderProps> = ({ children }) => {
  const [messages, setMessages] = useState<ToastMessage[]>([]);

  const generateId = () => {
    return `toast_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  };

  const removeMessage = (id: string) => {
    setMessages(prev => prev.filter(msg => msg.id !== id));
  };

  const showToast = (message: string, httpStatus?: number, duration?: number) => {
    // 根据HTTP状态码判断类型：2xx为成功，其他为错误
    const type = httpStatus && httpStatus >= 200 && httpStatus < 300 ? 'success' : 'error';
    
    const newMessage: ToastMessage = {
      id: generateId(),
      message,
      type,
      duration: duration || 3000
    };

    setMessages(prev => [...prev, newMessage]);
  };

  const showSuccess = (message: string, duration?: number) => {
    const newMessage: ToastMessage = {
      id: generateId(),
      message,
      type: 'success',
      duration: duration || 3000
    };

    setMessages(prev => [...prev, newMessage]);
  };

  const showError = (message: string, duration?: number) => {
    const newMessage: ToastMessage = {
      id: generateId(),
      message,
      type: 'error',
      duration: duration || 3000
    };

    setMessages(prev => [...prev, newMessage]);
  };

  return (
    <ToastContext.Provider value={{ showToast, showSuccess, showError }}>
      {children}
      <GlobalToast messages={messages} onRemove={removeMessage} />
    </ToastContext.Provider>
  );
};
