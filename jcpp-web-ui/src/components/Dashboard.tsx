/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {useEffect, useRef, useState} from 'react';
import {Alert, Button, Card, Col, Row, Spin, Statistic} from 'antd';
import {AimOutlined, EnvironmentOutlined, ReloadOutlined, ThunderboltOutlined} from '@ant-design/icons';
import * as echarts from 'echarts';
import {type DashboardStats, getDashboardStats} from '../services/dashboardService';
import {getErrorMessage} from '../services/api';
import {showMessage} from '../utils';

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const pileChartRef = useRef<HTMLDivElement>(null);
  const gunChartRef = useRef<HTMLDivElement>(null);
  const pileChartInstance = useRef<echarts.ECharts | null>(null);
  const gunChartInstance = useRef<echarts.ECharts | null>(null);

  // 加载仪表盘数据
  const loadDashboardStats = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getDashboardStats();
      setStats(data);
      console.log('Dashboard stats loaded:', data);
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      setError(errorMessage);
      showMessage.error(`加载仪表盘数据失败：${errorMessage}`);
      console.error('Dashboard stats loading failed:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardStats();
    // 每30秒自动刷新数据
    const interval = setInterval(loadDashboardStats, 30000);
    return () => clearInterval(interval);
  }, []);

  // 充电桩状态饼图
  useEffect(() => {
    if (!pileChartRef.current || loading || !stats?.pileStatusDistribution) return;

    // 销毁之前的图表实例
    if (pileChartInstance.current) {
      pileChartInstance.current.dispose();
    }

    // 创建新的图表实例
    const chart = echarts.init(pileChartRef.current);
    pileChartInstance.current = chart;

    const { pileStatusDistribution } = stats;
    const data = [
      { 
        name: '在线', 
        value: pileStatusDistribution.onlinePiles,
        itemStyle: { color: '#52c41a' }
      },
      { 
        name: '离线', 
        value: pileStatusDistribution.offlinePiles,
        itemStyle: { color: '#ff7875' }
      }
    ].filter(item => item.value > 0);

    const option = {
      backgroundColor: 'transparent',
      title: {
        text: '充电桩在线状态',
        left: 'center',
        top: 15,
        textStyle: {
          fontSize: 16,
          fontWeight: 'normal',
          color: '#262626'
        }
      },
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c}台 ({d}%)',
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        borderWidth: 0,
        textStyle: {
          color: '#fff',
          fontSize: 12
        }
      },
      legend: {
        orient: 'horizontal',
        bottom: 15,
        data: data.map(item => item.name),
        textStyle: {
          fontSize: 12,
          color: '#666'
        }
      },
      series: [
        {
          name: '充电桩状态',
          type: 'pie',
          radius: ['45%', '65%'],
          center: ['50%', '55%'],
          data: data,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.2)'
            }
          },
          label: {
            formatter: '{b}\n{c}台\n{d}%',
            fontSize: 11,
            color: '#666'
          },
          labelLine: {
            length: 10,
            length2: 5
          }
        }
      ]
    };

    chart.setOption(option);

    // 窗口大小变化时重新调整图表
    const handleResize = () => chart.resize();
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [stats, loading]);

  // 充电枪状态饼图
  useEffect(() => {
    if (!gunChartRef.current || loading || !stats?.gunStatusDistribution) return;

    // 销毁之前的图表实例
    if (gunChartInstance.current) {
      gunChartInstance.current.dispose();
    }

    // 创建新的图表实例
    const chart = echarts.init(gunChartRef.current);
    gunChartInstance.current = chart;

    const { gunStatusDistribution } = stats;
    
    // 准备数据 - 只显示有数据的状态
    const statusData = [
      { name: '空闲', value: gunStatusDistribution.idleGuns, color: '#52c41a' },
      { name: '已插枪', value: gunStatusDistribution.insertedGuns, color: '#faad14' },
      { name: '充电中', value: gunStatusDistribution.chargingGuns, color: '#1890ff' },
      { name: '充电完成', value: gunStatusDistribution.chargeCompleteGuns, color: '#13c2c2' },
      { name: '放电准备', value: gunStatusDistribution.dischargeReadyGuns, color: '#722ed1' },
      { name: '放电中', value: gunStatusDistribution.dischargingGuns, color: '#eb2f96' },
      { name: '放电完成', value: gunStatusDistribution.dischargeCompleteGuns, color: '#fa8c16' },
      { name: '预约', value: gunStatusDistribution.reservedGuns, color: '#a0d911' },
      { name: '故障', value: gunStatusDistribution.faultGuns, color: '#ff7875' }
    ].filter(item => item.value > 0);

    const data = statusData.map(item => ({
      name: item.name,
      value: item.value,
      itemStyle: { color: item.color }
    }));

    const option = {
      backgroundColor: 'transparent',
      title: {
        text: '充电枪运行状态',
        left: 'center',
        top: 15,
        textStyle: {
          fontSize: 16,
          fontWeight: 'normal',
          color: '#262626'
        }
      },
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          const percentage = ((params.value / gunStatusDistribution.totalGuns) * 100).toFixed(1);
          return `${params.name}<br/>数量: ${params.value}台<br/>占比: ${percentage}%`;
        },
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        borderWidth: 0,
        textStyle: {
          color: '#fff',
          fontSize: 12
        }
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle',
        data: data.map(item => item.name),
        textStyle: {
          fontSize: 11,
          color: '#666'
        },
        formatter: (name: string) => {
          const item = statusData.find(d => d.name === name);
          return item ? `${name} (${item.value})` : name;
        }
      },
      series: [
        {
          name: '充电枪状态',
          type: 'pie',
          radius: ['35%', '60%'],
          center: ['65%', '55%'],
          data: data,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.2)'
            }
          },
          label: {
            formatter: '{b}\n{d}%',
            fontSize: 10,
            color: '#666'
          },
          labelLine: {
            length: 8,
            length2: 3
          }
        }
      ]
    };

    chart.setOption(option);

    // 窗口大小变化时重新调整图表
    const handleResize = () => chart.resize();
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [stats, loading]);

  // 组件卸载时清理图表
  useEffect(() => {
    return () => {
      if (pileChartInstance.current) {
        pileChartInstance.current.dispose();
      }
      if (gunChartInstance.current) {
        gunChartInstance.current.dispose();
      }
    };
  }, []);

  // 首次加载状态
  if (loading && !stats) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" />
        <div style={{ marginTop: 16 }}>加载仪表盘数据中...</div>
      </div>
    );
  }

  // 错误状态
  if (error && !stats) {
    return (
      <div style={{ padding: '20px' }}>
        <Alert
          message="仪表盘加载失败"
          description={error}
          type="error"
          showIcon
          action={
            <Button size="small" danger onClick={loadDashboardStats}>
              重试
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', backgroundColor: '#fafafa', minHeight: '100vh' }}>
      {/* 标题区域 */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: 20 
      }}>
        <h1 style={{ 
          margin: 0, 
          fontSize: 20, 
          fontWeight: 500, 
          color: '#262626'
        }}>
          充电站管理仪表盘
        </h1>
        <ReloadOutlined 
          style={{ 
            fontSize: 16, 
            color: loading ? '#1890ff' : '#666',
            cursor: 'pointer',
            transition: 'color 0.3s'
          }}
          spin={loading}
          onClick={loadDashboardStats}
          title="刷新数据"
        />
      </div>
      
      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={8}>
          <Card 
            style={{ 
              borderRadius: '6px',
              border: '1px solid #f0f0f0',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
            }}
            bodyStyle={{ padding: '20px' }}
          >
            <Statistic
              title="充电站数量"
              value={stats?.overview?.totalStations || 0}
              prefix={<EnvironmentOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#262626', fontSize: '24px', fontWeight: '500' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card 
            style={{ 
              borderRadius: '6px',
              border: '1px solid #f0f0f0',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
            }}
            bodyStyle={{ padding: '20px' }}
          >
            <Statistic
              title="充电桩数量"
              value={stats?.overview?.totalPiles || 0}
              prefix={<ThunderboltOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#262626', fontSize: '24px', fontWeight: '500' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card 
            style={{ 
              borderRadius: '6px',
              border: '1px solid #f0f0f0',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
            }}
            bodyStyle={{ padding: '20px' }}
          >
            <Statistic
              title="充电枪数量"
              value={stats?.overview?.totalGuns || 0}
              prefix={<AimOutlined style={{ color: '#722ed1' }} />}
              valueStyle={{ color: '#262626', fontSize: '24px', fontWeight: '500' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 图表区域 */}
      {stats?.pileStatusDistribution && stats?.gunStatusDistribution ? (
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card 
              style={{ 
                borderRadius: '6px',
                border: '1px solid #f0f0f0',
                boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
                height: '380px'
              }}
              bodyStyle={{ padding: '16px', height: '100%' }}
              loading={loading}
            >
              <div 
                ref={pileChartRef} 
                style={{ 
                  height: '100%',
                  width: '100%'
                }}
              />
            </Card>
          </Col>
          
          <Col xs={24} lg={12}>
            <Card 
              style={{ 
                borderRadius: '6px',
                border: '1px solid #f0f0f0',
                boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
                height: '380px'
              }}
              bodyStyle={{ padding: '16px', height: '100%' }}
              loading={loading}
            >
              <div 
                ref={gunChartRef} 
                style={{ 
                  height: '100%',
                  width: '100%'
                }}
              />
            </Card>
          </Col>
        </Row>
      ) : (
        <Card 
          style={{ 
            borderRadius: '6px', 
            border: '1px solid #f0f0f0',
            boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
            textAlign: 'center', 
            padding: '40px 0' 
          }}
        >
          <div style={{ color: '#999', fontSize: '14px' }}>
            {stats ? '暂无图表数据' : '等待图表数据加载...'}
          </div>
        </Card>
      )}
    </div>
  );
};

export default Dashboard;