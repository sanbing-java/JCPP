/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {
    Button,
    Card,
    Checkbox,
    Col,
    Dropdown,
    Form,
    Input,
    Modal,
    Popconfirm,
    Row,
    Select,
    Space,
    Table,
    Tag,
    Typography
} from 'antd';
import {DeleteOutlined, PlusOutlined, TableOutlined} from '@ant-design/icons';
import type {ColumnsType, TableProps} from 'antd/es/table';
import {pileService} from '../services/pileService';
import * as stationService from '../services/stationService';
import * as protocolService from '../services/protocolService';
import {getErrorMessage} from '../services/api';
import {Pile, PileCreateRequest, PileQueryRequest, PileUpdateRequest, StationOption} from '../types';
import {
    debounce,
    formatTimestamp,
    generatePileCode,
    getProtocolText,
    getStatusColor,
    getStatusText,
    getTypeText,
    showMessage
} from '../utils';

const { confirm } = Modal;


const PileManagement: React.FC = () => {
    const navigate = useNavigate();
  
  // 状态管理
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<Pile[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalTitle, setModalTitle] = useState('');
  const [isEdit, setIsEdit] = useState(false);
  const [stationOptions, setStationOptions] = useState<StationOption[]>([]);
  const [stationLoading, setStationLoading] = useState(false);
  
  // 协议选项
  const [protocolOptions, setProtocolOptions] = useState<protocolService.ProtocolOption[]>([]);
  
  // 批量删除相关状态
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [batchDeleting, setBatchDeleting] = useState(false);

  // 表单实例
  const [form] = Form.useForm();
  const [searchForm] = Form.useForm();

  // 分页和搜索状态
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 条记录`
  });

  const [searchParams, setSearchParams] = useState<PileQueryRequest>({
    page: 1,  // 修改为从1开始，与后端保持一致
    size: 10
  });

  // 列可见性配置
  interface ColumnConfig {
    key: string;
    title: string;
    defaultVisible: boolean;
  }

  const columnConfigs: ColumnConfig[] = [
    { key: 'pileName', title: '充电桩名称', defaultVisible: true },
    { key: 'pileCode', title: '充电桩编码', defaultVisible: true },
    { key: 'protocol', title: '协议', defaultVisible: false },
    { key: 'brand', title: '品牌', defaultVisible: false },
    { key: 'model', title: '型号', defaultVisible: false },
    { key: 'manufacturer', title: '制造商', defaultVisible: false },
    { key: 'type', title: '类型', defaultVisible: false },
    { key: 'status', title: '状态', defaultVisible: true },
      {key: 'gunCount', title: '充电枪数量', defaultVisible: true},
    { key: 'connectedAt', title: '连接时间', defaultVisible: true },
    { key: 'disconnectedAt', title: '断线时间', defaultVisible: true },
    { key: 'lastActiveTime', title: '最后活跃时间', defaultVisible: true },
    { key: 'createdTime', title: '创建时间', defaultVisible: true },
    { key: 'updatedTime', title: '更新时间', defaultVisible: false },
  ];

  // 列可见性状态
  const [visibleColumns, setVisibleColumns] = useState<Record<string, boolean>>(() => {
    const defaultVisible: Record<string, boolean> = {};
    columnConfigs.forEach(config => {
      defaultVisible[config.key] = config.defaultVisible;
    });
    return defaultVisible;
  });

  // 列顺序状态（不包含action列，action列始终在最后）
  const [columnOrder, setColumnOrder] = useState<string[]>(() => {
    return columnConfigs.map(config => config.key);
  });

  // 完整的表格列定义
  const allColumns: ColumnsType<Pile> = useMemo(() => [
    {
      title: '充电桩名称',
      dataIndex: 'pileName',
      key: 'pileName',
      width: 150,
      sorter: true,
      render: (text: string) => (
        <div style={{ 
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap'
        }} title={text}>
          {text}
        </div>
      )
    },
    {
      title: '充电桩编码',
      dataIndex: 'pileCode',
      key: 'pileCode',
      width: 140,
      sorter: true,
      render: (text: string) => (
        <div style={{ 
          wordBreak: 'break-word', 
          whiteSpace: 'pre-wrap',
          lineHeight: '1.3'
        }}>
          {text}
        </div>
      )
    },
    {
      title: '协议',
      dataIndex: 'protocol',
      key: 'protocol',
      width: 110,
      render: (protocol: string) => getProtocolText(protocol)
    },
    {
      title: '品牌',
      dataIndex: 'brand',
      key: 'brand',
      width: 100,
      sorter: true
    },
    {
      title: '型号',
      dataIndex: 'model',
      key: 'model',
      width: 100
    },
    {
      title: '制造商',
      dataIndex: 'manufacturer',
      key: 'manufacturer',
      width: 120,
      sorter: true
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: 'AC' | 'DC') => (
        <Tag color={type === 'AC' ? 'blue' : 'orange'}>
          {getTypeText(type)}
        </Tag>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 65,
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>
          {getStatusText(status)}
        </Tag>
      )
    },
    {
        title: '充电枪数量',
        dataIndex: 'gunCount',
        key: 'gunCount',
        width: 100,
        sorter: true,
        render: (gunCount: number, record: Pile) => (
            <Button
                type="link"
                size="small"
                onClick={() => handleGunCountClick(record.pileCode)}
                style={{
                    padding: 0,
                    height: 'auto',
                    color: gunCount > 0 ? '#1890ff' : '#999'
                }}
            >
                {gunCount || 0}
            </Button>
        )
    },
      {
      title: '连接时间',
      dataIndex: 'connectedAt',
      key: 'connectedAt',
      width: 95,
      sorter: true,
      render: (timestamp: number) => {
        const formatted = formatTimestamp(timestamp);
        if (!formatted || formatted === '-') return formatted;
        const parts = formatted.split(' ');
        return (
          <div style={{ lineHeight: '1.3', fontSize: '13px' }}>
            <div>{parts[0]}</div>
            <div style={{ color: '#666' }}>{parts[1]}</div>
          </div>
        );
      }
    },
    {
      title: '断线时间',
      dataIndex: 'disconnectedAt',
      key: 'disconnectedAt',
      width: 95,
      sorter: true,
      render: (timestamp: number) => {
        const formatted = formatTimestamp(timestamp);
        if (!formatted || formatted === '-') return formatted;
        const parts = formatted.split(' ');
        return (
          <div style={{ lineHeight: '1.3', fontSize: '13px' }}>
            <div>{parts[0]}</div>
            <div style={{ color: '#666' }}>{parts[1]}</div>
          </div>
        );
      }
    },
    {
      title: '最后活跃',
      dataIndex: 'lastActiveTime',
      key: 'lastActiveTime',
      width: 95,
      sorter: true,
      render: (timestamp: number) => {
        const formatted = formatTimestamp(timestamp);
        if (!formatted || formatted === '-') return formatted;
        const parts = formatted.split(' ');
        return (
          <div style={{ lineHeight: '1.3', fontSize: '13px' }}>
            <div>{parts[0]}</div>
            <div style={{ color: '#666' }}>{parts[1]}</div>
          </div>
        );
      }
    },
    {
      title: '创建时间',
      dataIndex: 'createdTime',
      key: 'createdTime',
      width: 95,
      sorter: true,
      render: (timestamp: number) => {
        const formatted = formatTimestamp(timestamp);
        if (!formatted || formatted === '-') return formatted;
        const parts = formatted.split(' ');
        return (
          <div style={{ lineHeight: '1.3', fontSize: '13px' }}>
            <div>{parts[0]}</div>
            <div style={{ color: '#666' }}>{parts[1]}</div>
          </div>
        );
      }
    },
    {
      title: '更新时间',
      dataIndex: 'updatedTime',
      key: 'updatedTime',
      width: 95,
      sorter: true,
      render: (timestamp: number) => {
        const formatted = formatTimestamp(timestamp);
        if (!formatted || formatted === '-') return formatted;
        const parts = formatted.split(' ');
        return (
          <div style={{ lineHeight: '1.3', fontSize: '13px' }}>
            <div>{parts[0]}</div>
            <div style={{ color: '#666' }}>{parts[1]}</div>
          </div>
        );
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)} style={{ padding: '0 4px' }}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除充电桩"
            description={
              <div>
                <p>确定要删除充电桩 <strong>{record.pileName}</strong> 吗？</p>
                <p style={{ color: '#ff4d4f', margin: 0 }}>此操作不可撤销，请谨慎操作！</p>
              </div>
            }
            onConfirm={() => handleDelete(record)}
            okText="确定删除"
            okType="danger"
            cancelText="取消"
          >
            <Button type="link" size="small" danger style={{ padding: '0 4px' }}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  ], []);

  // 根据可见性和顺序过滤并排序列
  const visibleColumnsData = useMemo(() => {
    // 先按照用户定义的顺序排序（不包含action）
    const orderedColumns = columnOrder.map(key => {
      return allColumns.find(col => col.key === key);
    }).filter(Boolean) as ColumnsType<Pile>;
    
    // 过滤出可见的列
    const filtered = orderedColumns.filter(column => {
      return visibleColumns[column.key as string];
    });
    
    // 找到操作列并确保始终在最后
    const actionColumn = allColumns.find(col => col.key === 'action');
    
    return actionColumn ? [...filtered, actionColumn] : filtered;
  }, [visibleColumns, columnOrder, allColumns]);

  // 列选择器变更处理
  const handleColumnVisibilityChange = (checkedValues: string[]) => {
    const newVisibleColumns: Record<string, boolean> = {};
    columnConfigs.forEach(config => {
      newVisibleColumns[config.key] = checkedValues.includes(config.key);
    });
    setVisibleColumns(newVisibleColumns);
  };

  // 移动列顺序
  const moveColumn = (index: number, direction: 'up' | 'down') => {
    const visibleKeys = columnOrder.filter(key => visibleColumns[key]);
    const currentKey = visibleKeys[index];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    
    if (targetIndex >= 0 && targetIndex < visibleKeys.length) {
      const targetKey = visibleKeys[targetIndex];
      
      // 在原始顺序中交换位置
      const newOrder = [...columnOrder];
      const currentOriginalIndex = newOrder.indexOf(currentKey);
      const targetOriginalIndex = newOrder.indexOf(targetKey);
      
      [newOrder[currentOriginalIndex], newOrder[targetOriginalIndex]] = 
        [newOrder[targetOriginalIndex], newOrder[currentOriginalIndex]];
      
      setColumnOrder(newOrder);
    }
  };

  // 列选择器菜单
  const columnSelectorMenu = {
    items: [
      {
        key: 'column-selector',
        label: (
          <div style={{ padding: '8px 0', minWidth: 200 }} onClick={e => e.stopPropagation()}>
            <Typography.Text strong style={{ fontSize: 12 }}>自定义列显示</Typography.Text>
            
            {/* 列可见性选择 */}
            <div style={{ marginTop: 8, marginBottom: 12 }}>
              <Typography.Text style={{ fontSize: 11, color: '#666' }}>选择显示列：</Typography.Text>
              <Checkbox.Group
                value={Object.keys(visibleColumns).filter(key => visibleColumns[key])}
                onChange={handleColumnVisibilityChange}
                style={{ width: '100%' }}
              >
                <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  {columnOrder.map(key => {
                    const config = columnConfigs.find(c => c.key === key);
                    if (!config) return null;
                    return (
                      <Checkbox key={config.key} value={config.key} style={{ fontSize: 11 }}>
                        {config.title}
                      </Checkbox>
                    );
                  })}
                </div>
              </Checkbox.Group>
            </div>

            {/* 列顺序调整 */}
            <div>
              <Typography.Text style={{ fontSize: 11, color: '#666' }}>调整列顺序：</Typography.Text>
              <div style={{ maxHeight: 120, overflowY: 'auto', marginTop: 4 }}>
                {columnOrder.filter(key => visibleColumns[key]).map((key, index, visibleKeys) => {
                  const config = columnConfigs.find(c => c.key === key);
                  if (!config) return null;
                  
                  return (
                    <div key={key} style={{ 
                      display: 'flex', 
                      alignItems: 'center', 
                      padding: '2px 0',
                      fontSize: 11,
                      gap: 4
                    }}>
                      <span style={{ flex: 1, minWidth: 0 }}>{config.title}</span>
                      <Button 
                        type="text" 
                        size="small" 
                        onClick={() => moveColumn(index, 'up')}
                        disabled={index === 0}
                        style={{ padding: '0 4px', height: 20, fontSize: 10 }}
                      >
                        ↑
                      </Button>
                      <Button 
                        type="text" 
                        size="small" 
                        onClick={() => moveColumn(index, 'down')}
                        disabled={index === visibleKeys.length - 1}
                        style={{ padding: '0 4px', height: 20, fontSize: 10 }}
                      >
                        ↓
                      </Button>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        ),
      },
    ],
  };

  // 加载数据
  const loadData = async () => {
    setLoading(true);
    try {
      const response = await pileService.getPiles(searchParams);
      const { records, total } = response.data;
      setDataSource(records);
      setPagination(prev => ({
        ...prev,
        current: searchParams.page, // 现在页码从1开始，直接使用
        pageSize: searchParams.size,
        total
      }));
    } catch (error: any) {
      console.error('加载充电桩数据失败:', error);
      const errorMessage = getErrorMessage(error);
      showMessage.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // 搜索充电站选项（带防抖）
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const searchStationOptions = useCallback(
    debounce(async (keyword: string) => {
      setStationLoading(true);
      try {
        const response = await stationService.searchStationOptions({
          keyword,
          page: 1,  // 修改为从1开始
          size: 20
        });
        setStationOptions(Array.isArray(response) ? response : []);
      } catch (error: any) {
        console.error('搜索充电站选项失败:', error);
        showMessage.error('搜索充电站列表失败');
      } finally {
        setStationLoading(false);
      }
    }, 300),
    []
  );

  // 初始化加载充电站选项
  const loadInitialStationOptions = async () => {
    try {
      const response = await stationService.searchStationOptions({
        page: 1,  // 修改为从1开始
        size: 20
      });
      setStationOptions(Array.isArray(response) ? response : []);
    } catch (error: any) {
      console.error('加载充电站选项失败:', error);
      showMessage.error('加载充电站列表失败');
    }
  };

  // 加载协议选项
  const loadProtocolOptions = async () => {
    try {
      const protocols = await protocolService.getSupportedProtocols();
      setProtocolOptions(protocols);
    } catch (error: any) {
      console.error('加载协议选项失败:', error);
      showMessage.error('加载协议选项失败');
    }
  };

  // 表格变化处理
  const handleTableChange: TableProps<Pile>['onChange'] = (pag, filters, sorter) => {
    let newParams = {
      ...searchParams,
      page: pag.current || 1,  // 直接使用current，不再减1
      size: pag.pageSize || 10
    };

    // 处理排序
    if (sorter && !Array.isArray(sorter) && sorter.field) {
      newParams = {
        ...newParams,
        sortField: sorter.field as string,  // 修改为sortField
        sortOrder: sorter.order === 'ascend' ? 'asc' : 'desc'  // 修改为小写
      };
    } else {
      delete newParams.sortField;  // 修改为sortField
      delete newParams.sortOrder;
    }

    // 只更新搜索参数，让useEffect自动处理数据加载和分页状态同步
    setSearchParams(newParams);
  };

  // 搜索处理
  const handleSearch = (values: any) => {
    const newParams: PileQueryRequest = {
      page: 1,  // 搜索时重置为第1页
      size: pagination.pageSize,
      ...values
    };
    setSearchParams(newParams);
  };

  // 重置搜索
  const handleReset = () => {
    searchForm.resetFields();
    const newParams: PileQueryRequest = {
      page: 1,  // 重置时回到第1页
      size: pagination.pageSize
    };
    setSearchParams(newParams);
  };

  // 显示新建Modal
  const showCreateModal = async () => {
    form.resetFields();
    // 设置默认值
    form.setFieldsValue({
      protocol: 'yunkuaichongV150',
      type: 'AC'
    });
    setModalTitle('新建充电桩');
    setIsEdit(false);
    await loadInitialStationOptions();
    setModalVisible(true);
  };

  // 编辑充电桩
  const handleEdit = async (record: Pile) => {
    form.resetFields();
    form.setFieldsValue({
      id: record.id,
      pileName: record.pileName,
      pileCode: record.pileCode,
      protocol: record.protocol,
      type: record.type,
      brand: record.brand,
      model: record.model,
      manufacturer: record.manufacturer,
      stationId: record.stationId
    });
    setModalTitle('编辑充电桩');
    setIsEdit(true);
    await loadInitialStationOptions();
    setModalVisible(true);
  };

    // 处理充电枪数量点击
    const handleGunCountClick = (pileCode: string) => {
        // 跳转到充电枪管理页面，并设置搜索条件
        navigate('/page/guns', {
            state: {
                searchPileCode: pileCode
            }
        });
    };


  // 生成充电桩编码
  const handleGeneratePileCode = () => {
    const code = generatePileCode();
    form.setFieldValue('pileCode', code);
  };

  // Modal确认
  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      let response;
      
      if (isEdit) {
        // 编辑
        const updateData: PileUpdateRequest = {
          pileName: values.pileName,
          protocol: values.protocol,
          type: values.type,
          brand: values.brand,
          model: values.model,
          manufacturer: values.manufacturer,
          stationId: values.stationId
        };
        response = await pileService.updatePile(values.id, updateData);
        showMessage.success(`充电桩 "${values.pileName}" 更新成功`);
      } else {
        // 新建
        const createData: PileCreateRequest = {
          pileName: values.pileName,
          pileCode: values.pileCode,
          protocol: values.protocol,
          type: values.type,
          brand: values.brand,
          model: values.model,
          manufacturer: values.manufacturer,
          stationId: values.stationId
        };
        response = await pileService.createPile(createData);
        showMessage.success(`充电桩 "${values.pileName}" 创建成功`);
      }
      
      setModalVisible(false);
      // 清空选择状态并重新加载数据
      setSelectedRowKeys([]);
      loadData();
    } catch (error: any) {
      console.error('操作失败:', error);
      
      // 获取错误消息和HTTP状态码
      const errorMessage = error?.response?.data?.message || getErrorMessage(error) || '操作失败，请重试';
      const httpStatus = error?.response?.status || 500;
      
      // 使用Toast显示错误消息
      showMessage.error(errorMessage);
    }
  };

  // Modal取消
  const handleModalCancel = () => {
    setModalVisible(false);
    form.resetFields();
  };

  // 删除充电桩
  const handleDelete = async (record: Pile) => {
    try {
      console.log('开始删除充电桩:', record.pileName, 'ID:', record.id);
      const response = await pileService.deletePile(record.id);
      console.log('删除充电桩成功:', record.pileName, 'response:', response);
      showMessage.success(`充电桩 "${record.pileName}" 删除成功`);
      // 清空选择状态并重新加载数据
      setSelectedRowKeys([]);
      loadData();
    } catch (error: any) {
      console.error('删除充电桩失败:', error);
      console.error('错误详情:', {
        response: error?.response,
        data: error?.response?.data,
        status: error?.response?.status,
        message: error?.message
      });
      
      // 获取错误消息和HTTP状态码
      const baseErrorMessage = getErrorMessage(error);
      console.log('处理后的错误消息:', baseErrorMessage);
      
      const errorMessage = `删除充电桩 "${record.pileName}" 失败：${baseErrorMessage}`;
      const httpStatus = error?.response?.status || 500;
      
      // 使用Toast显示错误消息
      showMessage.error(errorMessage);
    }
  };

  // 批量删除
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      showMessage.warning('请先选择要删除的记录');
      return;
    }
    
    confirm({
      title: '确认批量删除',
      content: (
        <div>
          <p>您确定要删除选中的 <strong style={{ color: '#ff4d4f' }}>{selectedRowKeys.length}</strong> 条充电桩吗？</p>
          <p style={{ color: '#ff4d4f', marginTop: 8 }}>⚠️ 此操作不可撤销，请谨慎操作！</p>
        </div>
      ),
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      width: 420,
      centered: true,
      onOk: async () => {
        setBatchDeleting(true);
        let successCount = 0;
        let failCount = 0;
        const failedNames: string[] = [];
        const failedReasons: string[] = [];

        try {
          // 使用 for...of 循环按顺序删除
          for (const key of selectedRowKeys) {
            try {
              await pileService.deletePile(key as string);
              successCount++;
              
              // 每删除一个都更新进度提示
              if (selectedRowKeys.length > 3) {
                showMessage.loading(`正在删除... (${successCount}/${selectedRowKeys.length})`);
              }
            } catch (error: any) {
              failCount++;
              const record = dataSource.find(item => item.id === key);
              const pileName = record?.pileName || `ID: ${key}`;
              failedNames.push(pileName);
              
              // 获取详细错误信息
              const errorMessage = getErrorMessage(error);
              failedReasons.push(`${pileName}: ${errorMessage}`);
            }
          }

          // 显示删除结果
          if (failCount === 0) {
            showMessage.success(`批量删除成功，共删除 ${successCount} 条充电桩`);
          } else if (successCount === 0) {
            // 全部失败
            showMessage.error(
              `批量删除失败，所有 ${failCount} 条充电桩都删除失败。失败原因：${failedReasons.join('; ')}`
            );
          } else {
            // 部分成功
            showMessage.warning(
              `删除完成：成功 ${successCount} 条，失败 ${failCount} 条。失败原因：${failedReasons.join('; ')}`
            );
          }

          // 重新加载数据并清空选择
          setSelectedRowKeys([]);
          loadData();
        } catch (error: any) {
          // 处理整体操作异常
          const errorMessage = getErrorMessage(error);
          showMessage.error(`批量删除操作失败：${errorMessage}`);
        } finally {
          setBatchDeleting(false);
        }
      }
    });
  };

  // 行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(newSelectedRowKeys);
    },
    onSelectAll: (selected: boolean, selectedRows: Pile[], changeRows: Pile[]) => {
      console.log('onSelectAll:', selected, selectedRows, changeRows);
    },
    onSelect: (record: Pile, selected: boolean, selectedRows: Pile[]) => {
      console.log('onSelect:', record, selected, selectedRows);
    },
  };

  // 充电站选项过滤函数
  const filterStationOption = (input: string, option: any) => {
    const label = option.children;
    return label.toLowerCase().includes(input.toLowerCase());
  };

  // 组件挂载时加载数据
  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  useEffect(() => {
    loadInitialStationOptions();
    loadProtocolOptions();
  }, []);

  return (
    <div style={{ padding: '0' }}>
      {/* 页面头部 */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: 16 
      }}>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600, color: '#262626' }}>
          充电桩管理
        </h2>
        <Space>
          {selectedRowKeys.length > 0 && (
            <Button 
              danger 
              icon={<DeleteOutlined />} 
              onClick={handleBatchDelete}
              loading={batchDeleting}
            >
              批量删除 ({selectedRowKeys.length})
            </Button>
          )}
          <Button type="primary" icon={<PlusOutlined />} onClick={showCreateModal}>
            新建充电桩
          </Button>
        </Space>
      </div>

      {/* 搜索表单 */}
      <Card style={{ marginBottom: 16 }}>
        <Form
          form={searchForm}
          onFinish={handleSearch}
        >
          <Row gutter={[16, 16]}>
            <Col span={6}>
              <Form.Item label="充电桩名称" name="pileName" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入充电桩名称"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="充电桩编码" name="pileCode" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入充电桩编码"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="品牌" name="brand" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入品牌"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="制造商" name="manufacturer" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入制造商"
                  allowClear
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col span={6}>
              <Form.Item label="协议" name="protocol" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择协议"
                  allowClear
                  showSearch
                  optionFilterProp="children"
                  filterOption={(input, option) =>
                    String(option?.children || '').toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }
                >
                  {protocolOptions.map(protocol => (
                    <Select.Option key={protocol.value} value={protocol.value}>
                      {protocol.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="型号" name="model" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入型号"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="类型" name="type" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择类型"
                  allowClear
                >
                  <Select.Option value="AC">交流桩</Select.Option>
                  <Select.Option value="DC">直流桩</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="状态" name="status" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择状态"
                  allowClear
                >
                  <Select.Option value="ONLINE">在线</Select.Option>
                  <Select.Option value="OFFLINE">离线</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row style={{ marginTop: 16 }}>
            <Col span={24} style={{ textAlign: 'right' }}>
              <Space>
                <Button onClick={handleReset}>重置</Button>
                <Button type="primary" htmlType="submit">搜索</Button>
              </Space>
            </Col>
          </Row>
        </Form>
      </Card>

      {/* 数据表格 */}
      <Card
        title="充电桩列表"
        extra={
          <Dropdown 
            menu={columnSelectorMenu} 
            placement="bottomRight" 
            trigger={['click']}
            overlayStyle={{ minWidth: 180 }}
          >
            <Button 
              icon={<TableOutlined />} 
              type="text" 
              size="small"
              style={{ padding: '4px 8px' }}
              title="自定义列"
            />
          </Dropdown>
        }
      >
        <Table
          rowSelection={rowSelection}
          columns={visibleColumnsData}
          dataSource={dataSource}
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
          }}
          onChange={handleTableChange}
          rowKey="id"
          size="small"
          scroll={{ x: 900 }}
        />
      </Card>

      {/* 新增/编辑充电桩Modal */}
      <Modal
        title={modalTitle}
        open={modalVisible}
        width={800}
        onOk={handleModalOk}
        onCancel={handleModalCancel}
      >
        <Form
          form={form}
          labelCol={{ span: 6 }}
          wrapperCol={{ span: 16 }}
        >
          {isEdit && (
            <Form.Item name="id" hidden>
              <Input />
            </Form.Item>
          )}
          
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="充电桩名称"
                name="pileName"
                rules={[{ required: true, message: '请输入充电桩名称' }]}
              >
                <Input placeholder="请输入充电桩名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="充电桩编码"
                name="pileCode"
                rules={[{ required: true, message: '请输入充电桩编码' }]}
              >
                <Input
                  placeholder="请输入充电桩编码"
                  disabled={isEdit}
                  suffix={
                    <Button
                      type="link"
                      size="small"
                      onClick={handleGeneratePileCode}
                      disabled={isEdit}
                      style={{ 
                        height: '24px',
                        lineHeight: '24px',
                        padding: '0 8px',
                        fontSize: '12px',
                        color: '#1890ff',
                        fontWeight: 500,
                        border: 'none',
                        background: 'transparent',
                        boxShadow: 'none'
                      }}
                    >
                      生成
                    </Button>
                  }
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="协议"
                name="protocol"
                rules={[{ required: true, message: '请选择协议' }]}
              >
                <Select placeholder="请选择协议">
                  {protocolOptions.map(protocol => (
                    <Select.Option key={protocol.value} value={protocol.value}>
                      {protocol.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="类型"
                name="type"
                rules={[{ required: true, message: '请选择类型' }]}
              >
                <Select placeholder="请选择类型">
                  <Select.Option value="AC">交流桩</Select.Option>
                  <Select.Option value="DC">直流桩</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="品牌" name="brand">
                <Input placeholder="请输入品牌" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="型号" name="model">
                <Input placeholder="请输入型号" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="制造商" name="manufacturer">
                <Input placeholder="请输入制造商" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="充电站"
                name="stationId"
                rules={[{ required: true, message: '请选择充电站' }]}
              >
                <Select
                  placeholder="请选择充电站"
                  showSearch
                  loading={stationLoading}
                  onSearch={searchStationOptions}
                  filterOption={filterStationOption}
                  allowClear
                >
                  {(stationOptions || []).map(station => (
                    <Select.Option key={station.id} value={station.id}>
                      {station.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default PileManagement;
