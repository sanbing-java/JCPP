/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {useEffect, useState} from 'react';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {Alert, Breadcrumb, Button, Card, Descriptions, Divider, message, Space, Spin, Typography} from 'antd';
import {ArrowLeftOutlined, BugOutlined, HomeOutlined, PlayCircleOutlined} from '@ant-design/icons';
import {Gun} from '../types';
import * as gunService from '../services/gunService';
import {api, getErrorMessage} from '../services/api';

const {Title, Text} = Typography;

interface DebugResult {
    url: string;
    method: string;
    headers: Record<string, string>;
    requestBody: any;
    response: any;
    timestamp: string;
    status: 'success' | 'error';
}

const GunDebug: React.FC = () => {
    const {gunCode} = useParams<{ gunCode: string }>();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const [gun, setGun] = useState<Gun | null>(null);
    const [loading, setLoading] = useState(true);
    const [debugLoading, setDebugLoading] = useState(false);
    const [debugResult, setDebugResult] = useState<DebugResult | null>(null);
    const [pileDebugLoading, setPileDebugLoading] = useState(false);
    const [pileDebugResult, setPileDebugResult] = useState<DebugResult | null>(null);

    // 获取返回URL，如果没有则使用默认的充电枪管理页面
    const returnUrl = searchParams.get('returnUrl') || '/page/guns';

    // 面包屑配置 - 按照官方文档标准写法
    const breadcrumbItems = [
        {
            title: (
                <span>
          <HomeOutlined/>
          <span style={{marginLeft: 8}}>首页</span>
        </span>
            ),
            onClick: () => navigate('/page/dashboard'),
        },
        {
            title: '充电枪管理',
            onClick: () => navigate(returnUrl),
        },
        {
            title: (
                <span>
          <BugOutlined/>
          <span style={{marginLeft: 8}}>充电枪调试</span>
        </span>
            ),
        },
    ];

    // 加载充电枪信息
    useEffect(() => {
        const loadGunInfo = async () => {
            if (!gunCode) return;

            try {
                setLoading(true);
                const gun = await gunService.getGunByCode(gunCode);
                setGun(gun);
            } catch (error: any) {
                console.error('加载充电枪信息失败:', error);
                const errorMessage = getErrorMessage(error);
                message.error(`加载充电枪信息失败: ${errorMessage}`);
                navigate('/page/guns');
            } finally {
                setLoading(false);
            }
        };

        loadGunInfo();
    }, [gunCode, navigate]);

    // 返回充电枪列表，保持原有的查询参数
    const handleBack = () => {
        navigate(returnUrl);
    };

    // 执行充电枪状态查询调试
    const handleDebugGunStatus = async () => {
        if (!gun) return;

        // 清除充电桩调试结果，只显示充电枪调试结果
        setPileDebugResult(null);
        setDebugLoading(true);
        try {
            const headers = {
                'Content-Type': 'application/json',
            };

            // 直接调用现有的正确API接口
            const response = await api.get(`/api/guns/status/${gun.gunCode}`);

            setDebugResult({
                url: `/api/guns/status/${gun.gunCode}`,
                method: 'GET',
                headers,
                requestBody: null,
                response: response.data,
                timestamp: new Date().toLocaleString(),
                status: response.data.success ? 'success' : 'error'
            });

            if (response.data.success) {
                message.success('调试执行成功');
            } else {
                message.error('调试执行失败');
            }
        } catch (error: any) {
            const errorResult = {
                url: `/api/guns/status/${gun.gunCode}`,
                method: 'GET',
                headers: {'Content-Type': 'application/json'},
                requestBody: null,
                response: {success: false, message: getErrorMessage(error)},
                timestamp: new Date().toLocaleString(),
                status: 'error' as const
            };

            setDebugResult(errorResult);
            message.error('调试执行失败');
        } finally {
            setDebugLoading(false);
        }
    };

    // 执行充电桩状态查询调试
    const handleDebugPileStatus = async () => {
        if (!gun || !gun.pileCode) {
            message.error('充电桩编码不存在');
            return;
        }

        // 清除充电枪调试结果，只显示充电桩调试结果
        setDebugResult(null);
        setPileDebugLoading(true);
        try {
            const headers = {
                'Content-Type': 'application/json',
            };

            // 直接调用现有的正确API接口
            const response = await api.get(`/api/piles/status/${gun.pileCode}`);

            setPileDebugResult({
                url: `/api/piles/status/${gun.pileCode}`,
                method: 'GET',
                headers,
                requestBody: null,
                response: response.data,
                timestamp: new Date().toLocaleString(),
                status: response.data.success ? 'success' : 'error'
            });

            if (response.data.success) {
                message.success('调试执行成功');
            } else {
                message.error('调试执行失败');
            }
        } catch (error: any) {
            const errorResult = {
                url: `/api/piles/status/${gun.pileCode}`,
                method: 'GET',
                headers: {'Content-Type': 'application/json'},
                requestBody: null,
                response: {success: false, message: getErrorMessage(error)},
                timestamp: new Date().toLocaleString(),
                status: 'error' as const
            };

            setPileDebugResult(errorResult);
            message.error('调试执行失败');
        } finally {
            setPileDebugLoading(false);
        }
    };

    if (loading) {
        return (
            <div style={{padding: 24, textAlign: 'center'}}>
                <Spin size="large"/>
            </div>
        );
    }

    if (!gun) {
        return (
            <div style={{padding: 24}}>
                <Alert
                    message="充电枪不存在"
                    description="未找到指定的充电枪信息"
                    type="error"
                    showIcon
                />
            </div>
        );
    }

    return (
        <div style={{padding: 24}}>
            {/* 面包屑导航 */}
            <Breadcrumb
                items={breadcrumbItems}
                style={{marginBottom: 16}}
            />

            {/* 页面标题和返回按钮 */}
            <div style={{marginBottom: 24, display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                <Title level={3} style={{margin: 0}}>
                    <BugOutlined style={{marginRight: 8}}/>
                    充电枪调试 - {gun.gunName}
                </Title>
                <Button
                    icon={<ArrowLeftOutlined/>}
                    onClick={handleBack}
                >
                    返回列表
                </Button>
            </div>

            {/* 充电枪基本信息 */}
            <Card title="充电枪基本信息" style={{marginBottom: 24}}>
                <Descriptions column={2} bordered>
                    <Descriptions.Item label="充电枪名称">{gun.gunName}</Descriptions.Item>
                    <Descriptions.Item label="充电枪编码">{gun.gunCode}</Descriptions.Item>
                    <Descriptions.Item label="充电枪编号">{gun.gunNo}</Descriptions.Item>
                    <Descriptions.Item label="所属充电桩">{gun.pileName}</Descriptions.Item>
                    <Descriptions.Item label="充电桩编码">{gun.pileCode}</Descriptions.Item>
                    <Descriptions.Item label="所属充电站">{gun.stationName}</Descriptions.Item>
                    <Descriptions.Item label="创建时间">
                        {gun.createdTime ? new Date(gun.createdTime).toLocaleString() : '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="更新时间">
                        {gun.updatedTime ? new Date(gun.updatedTime).toLocaleString() : '-'}
                    </Descriptions.Item>
                </Descriptions>
            </Card>

            {/* 调试操作区域 */}
            <Card title="调试操作" style={{marginBottom: 24}}>
                <Space direction="vertical" style={{width: '100%'}}>
                    <div>
                        <Text strong>接口调试</Text>
                        <div style={{marginTop: 8}}>
                            <Space>
                                <Button
                                    type="primary"
                                    icon={<PlayCircleOutlined/>}
                                    loading={debugLoading}
                                    onClick={handleDebugGunStatus}
                                >
                                    查询充电枪状态
                                </Button>
                                <Button
                                    type="primary"
                                    icon={<PlayCircleOutlined/>}
                                    loading={pileDebugLoading}
                                    onClick={handleDebugPileStatus}
                                >
                                    查询充电桩状态
                                </Button>
                            </Space>
                        </div>
                    </div>
                </Space>
            </Card>

            {/* 充电枪调试结果展示 */}
            {debugResult && (
                <Card title="充电枪状态调试结果">
                    <Space direction="vertical" style={{width: '100%'}}>
                        <Alert
                            message={debugResult.status === 'success' ? '调试成功' : '调试失败'}
                            type={debugResult.status === 'success' ? 'success' : 'error'}
                            showIcon
                        />

                        <Descriptions column={1} bordered size="small">
                            <Descriptions.Item label="请求URL">
                                <Text code>{debugResult.url}</Text>
                            </Descriptions.Item>
                            <Descriptions.Item label="请求方法">
                                <Text code>{debugResult.method}</Text>
                            </Descriptions.Item>
                            <Descriptions.Item label="请求时间">
                                {debugResult.timestamp}
                            </Descriptions.Item>
                        </Descriptions>

                        <Divider orientation="left">请求头 (Headers)</Divider>
                        <pre style={{
                            background: '#f5f5f5',
                            padding: 12,
                            borderRadius: 4,
                            fontSize: 12,
                            overflow: 'auto'
                        }}>
              {JSON.stringify(debugResult.headers, null, 2)}
            </pre>

                        <Divider orientation="left">请求体 (Request Body)</Divider>
                        <pre style={{
                            background: '#f5f5f5',
                            padding: 12,
                            borderRadius: 4,
                            fontSize: 12,
                            overflow: 'auto'
                        }}>
              {JSON.stringify(debugResult.requestBody, null, 2)}
            </pre>

                        <Divider orientation="left">响应结果 (Response)</Divider>
                        <pre style={{
                            background: debugResult.status === 'success' ? '#f6ffed' : '#fff2f0',
                            padding: 12,
                            borderRadius: 4,
                            fontSize: 12,
                            overflow: 'auto',
                            border: `1px solid ${debugResult.status === 'success' ? '#b7eb8f' : '#ffccc7'}`
                        }}>
              {JSON.stringify(debugResult.response, null, 2)}
            </pre>
                    </Space>
                </Card>
            )}

            {/* 充电桩调试结果展示 */}
            {pileDebugResult && (
                <Card title="充电桩状态调试结果" style={{marginTop: 16}}>
                    <Space direction="vertical" style={{width: '100%'}}>
                        <Alert
                            message={pileDebugResult.status === 'success' ? '调试成功' : '调试失败'}
                            type={pileDebugResult.status === 'success' ? 'success' : 'error'}
                            showIcon
                        />

                        <Descriptions column={1} bordered size="small">
                            <Descriptions.Item label="请求URL">
                                <Text code>{pileDebugResult.url}</Text>
                            </Descriptions.Item>
                            <Descriptions.Item label="请求方法">
                                <Text code>{pileDebugResult.method}</Text>
                            </Descriptions.Item>
                            <Descriptions.Item label="请求时间">
                                {pileDebugResult.timestamp}
                            </Descriptions.Item>
                        </Descriptions>

                        <Divider orientation="left">请求头 (Headers)</Divider>
                        <pre style={{
                            background: '#f5f5f5',
                            padding: 12,
                            borderRadius: 4,
                            fontSize: 12,
                            overflow: 'auto'
                        }}>
              {JSON.stringify(pileDebugResult.headers, null, 2)}
            </pre>

                        <Divider orientation="left">请求体 (Request Body)</Divider>
                        <pre style={{
                            background: '#f5f5f5',
                            padding: 12,
                            borderRadius: 4,
                            fontSize: 12,
                            overflow: 'auto'
                        }}>
              {JSON.stringify(pileDebugResult.requestBody, null, 2)}
            </pre>

                        <Divider orientation="left">响应结果 (Response)</Divider>
                        <pre style={{
                            background: pileDebugResult.status === 'success' ? '#f6ffed' : '#fff2f0',
                            padding: 12,
                            borderRadius: 4,
                            fontSize: 12,
                            overflow: 'auto',
                            border: `1px solid ${pileDebugResult.status === 'success' ? '#b7eb8f' : '#ffccc7'}`
                        }}>
              {JSON.stringify(pileDebugResult.response, null, 2)}
            </pre>
                    </Space>
                </Card>
            )}
        </div>
    );
};

export default GunDebug;
