/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {useEffect, useMemo, useState} from 'react';
import {useLocation, useNavigate, useSearchParams} from 'react-router-dom';
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
import {
    BugOutlined,
    DeleteOutlined,
    PlusOutlined,
    ReloadOutlined,
    SearchOutlined,
    TableOutlined
} from '@ant-design/icons';
import type {ColumnsType, TableProps} from 'antd/es/table';
import {formatTimestamp, generateGunCode, showMessage} from '../utils';
import {getErrorMessage} from '../services/api';
import * as gunService from '../services/gunService';
import * as stationService from '../services/stationService';
import {pileService} from '../services/pileService';
import type {Gun, GunCreateRequest, GunUpdateRequest, PileOption, StationOption} from '../types';

const { confirm } = Modal;

const GunManagement: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [urlSearchParams, setUrlSearchParams] = useSearchParams();
  const [dataSource, setDataSource] = useState<Gun[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchForm] = Form.useForm();
  const [form] = Form.useForm();
  const [stationOptions, setStationOptions] = useState<StationOption[]>([]);
  const [pileOptions, setPileOptions] = useState<PileOption[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<Gun | null>(null);

  // 分页和搜索状态
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 条记录`
  });

    // 从URL参数初始化搜索参数
    const initSearchParams = (): {
        page: number;
        size: number;
        gunName?: string;
        gunCode?: string;
        gunNo?: string;
        stationId?: string;
        sortField?: string;
        sortOrder?: string;
    } => {
        return {
            page: parseInt(urlSearchParams.get('page') || '1'),
            size: parseInt(urlSearchParams.get('size') || '10'),
            gunName: urlSearchParams.get('gunName') || undefined,
            gunCode: urlSearchParams.get('gunCode') || undefined,
            gunNo: urlSearchParams.get('gunNo') || undefined,
            stationId: urlSearchParams.get('stationId') || undefined,
            sortField: urlSearchParams.get('sortField') || undefined,
            sortOrder: urlSearchParams.get('sortOrder') || undefined,
        };
    };

  const [searchParams, setSearchParams] = useState<{
    page: number;
    size: number;
    gunName?: string;
    gunCode?: string;
    gunNo?: string;
    stationId?: string;
    sortField?: string;
    sortOrder?: string;
  }>({
    page: 1,
    size: 10
  });

  // 批量删除相关状态
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [batchDeleting, setBatchDeleting] = useState(false);

  // 列可见性配置
  interface ColumnConfig {
    key: string;
    title: string;
    defaultVisible: boolean;
  }

  const columnConfigs: ColumnConfig[] = [
    { key: 'gunName', title: '充电枪名称', defaultVisible: true },
    { key: 'gunCode', title: '充电枪编码', defaultVisible: true },
    { key: 'gunNo', title: '枪号', defaultVisible: true },
    { key: 'stationName', title: '所属充电站', defaultVisible: true },
    { key: 'pileName', title: '所属充电桩', defaultVisible: true },
    { key: 'runStatus', title: '运行状态', defaultVisible: true },
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
  const allColumns: ColumnsType<Gun> = useMemo(() => [
    {
      title: '充电枪名称',
      dataIndex: 'gunName',
      key: 'gunName',
      width: 200,
      sorter: true,
    },
    {
      title: '充电枪编码',
      dataIndex: 'gunCode',
      key: 'gunCode',
      width: 150,
      sorter: true,
    },
    {
      title: '枪号',
      dataIndex: 'gunNo',
      key: 'gunNo',
      width: 55,
      sorter: true,
    },
    {
      title: '所属充电站',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 150,
      sorter: true,
      render: (stationName: string) => stationName || '-',
    },
    {
      title: '所属充电桩',
      dataIndex: 'pileName',
      key: 'pileName',
      width: 150,
      sorter: true,
      render: (pileName: string, record: Gun) => (
        <div>
          <div style={{ fontWeight: 500 }}>{pileName || record.pileCode || '-'}</div>
          {record.pileCode && pileName && (
            <div style={{ fontSize: '12px', color: '#666' }}>{record.pileCode}</div>
          )}
        </div>
      ),
    },
    {
      title: '运行状态',
      dataIndex: 'runStatus',
      key: 'runStatus',
      width: 100,
      render: (status: string) => {
        const getRunStatusColor = (status: string) => {
          const colors: Record<string, string> = {
            'IDLE': 'green',
            'INSERTED': 'orange', 
            'CHARGING': 'blue',
            'CHARGE_COMPLETE': 'cyan',
            'DISCHARGE_READY': 'purple',
            'DISCHARGING': 'magenta',
            'DISCHARGE_COMPLETE': 'lime',
            'RESERVED': 'geekblue',
            'FAULT': 'red'
          };
          return colors[status] || 'default';
        };

        const getRunStatusText = (status: string) => {
          const texts: Record<string, string> = {
            'IDLE': '空闲',
            'INSERTED': '已插枪',
            'CHARGING': '充电中',
            'CHARGE_COMPLETE': '充电完成',
            'DISCHARGE_READY': '放电准备',
            'DISCHARGING': '放电中',
            'DISCHARGE_COMPLETE': '放电完成',
            'RESERVED': '预约中',
            'FAULT': '故障'
          };
          return texts[status] || status;
        };
        
        return <Tag color={getRunStatusColor(status)}>{getRunStatusText(status)}</Tag>;
      },
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
      width: 150,
      fixed: 'right',
      render: (record: Gun) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
            <Button
                type="link"
                size="small"
                icon={<BugOutlined/>}
                onClick={() => handleDebug(record)}
            >
                调试
          </Button>
          <Popconfirm
            title="确认删除充电枪"
            description={
              <div>
                <p>确定要删除充电枪 <strong>{record.gunName}</strong> 吗？</p>
                <p style={{ color: '#ff4d4f', margin: 0 }}>此操作不可撤销，请谨慎操作！</p>
              </div>
            }
            onConfirm={() => handleDelete(record)}
            okText="确定删除"
            okType="danger"
            cancelText="取消"
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  // eslint-disable-next-line react-hooks/exhaustive-deps
  ], []);

  // 根据可见性和顺序过滤并排序列
  const visibleColumnsData = useMemo(() => {
    // 先按照用户定义的顺序排序（不包含action）
    const orderedColumns = columnOrder.map(key => {
      return allColumns.find(col => col.key === key);
    }).filter(Boolean) as ColumnsType<Gun>;
    
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
      const response = await gunService.getGuns(searchParams);
      const { records, total } = response;
      setDataSource(records);
      setPagination(prev => ({
        ...prev,
        current: searchParams.page,
        pageSize: searchParams.size,
        total
      }));
    } catch (error: any) {
      console.error('加载充电枪数据失败:', error);
      const errorMessage = getErrorMessage(error);
      showMessage.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // 加载充电站选项
  const loadStationOptions = async () => {
    try {
      const response = await stationService.getStationOptions();
      setStationOptions(Array.isArray(response) ? response : []);
    } catch (error: any) {
      console.error('加载充电站选项失败:', error);
    }
  };

  // 加载充电桩选项
  const loadPileOptions = async () => {
    try {
      const response = await pileService.getPileOptions();
      setPileOptions(response.data || []);
    } catch (error: any) {
      console.error('加载充电桩选项失败:', error);
    }
  };

    // 更新搜索参数的函数，同时更新URL
    const updateSearchParams = (newParams: any) => {
        setSearchParams(newParams);

        // 更新URL参数
        const urlParams = new URLSearchParams();
        Object.entries(newParams).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                urlParams.set(key, String(value));
            }
        });
        setUrlSearchParams(urlParams);
    };

    // 标记是否已经初始化URL参数
    const [urlParamsInitialized, setUrlParamsInitialized] = useState(false);

  // 初始化加载充电站选项和充电桩选项
  useEffect(() => {
    loadStationOptions();
    loadPileOptions();
  }, []);

    // 组件挂载后立即从URL参数初始化搜索参数
    useEffect(() => {
        const urlParams = initSearchParams();
        // 只有当URL参数与当前searchParams不同时才更新
        const hasUrlParams = urlParams.gunName || urlParams.gunCode || urlParams.gunNo ||
            urlParams.stationId || urlParams.sortField || urlParams.sortOrder ||
            urlParams.page !== 1 || urlParams.size !== 10;

        if (hasUrlParams) {
            console.log('从URL初始化搜索参数:', urlParams);
            setSearchParams(urlParams);
        }
        setUrlParamsInitialized(true);
    }, [urlSearchParams]); // 依赖urlSearchParams，确保URL变化时重新初始化

    // 监听搜索参数变化，但只有在URL参数初始化完成后才加载数据
    useEffect(() => {
        if (urlParamsInitialized) {
            loadData();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [searchParams, urlParamsInitialized]);

    // 初始化表单值（从URL参数）
    useEffect(() => {
        const initialValues = {
            gunName: searchParams.gunName || '',
            gunCode: searchParams.gunCode || '',
            gunNo: searchParams.gunNo || '',
            stationId: searchParams.stationId || '',
        };
        searchForm.setFieldsValue(initialValues);
    }, [searchForm, searchParams.gunName, searchParams.gunCode, searchParams.gunNo, searchParams.stationId]);

    // 初始化时如果URL有搜索参数，需要触发一次数据加载
    useEffect(() => {
        // 检查是否有搜索条件（除了page和size之外的参数）
        const hasSearchConditions = searchParams.gunName || searchParams.gunCode ||
            searchParams.gunNo || searchParams.stationId;

        if (hasSearchConditions) {
            // 如果有搜索条件，确保数据会被重新加载
            // 这里不需要手动调用loadData，因为searchParams的变化会触发useEffect中的loadData
            console.log('检测到URL搜索参数，将自动加载数据:', searchParams);
        }
    }, []); // 只在组件初始化时执行一次

    // 处理从充电桩管理页面传来的搜索参数
    useEffect(() => {
        const state = location.state as { searchPileCode?: string } | null;
        if (state?.searchPileCode) {
            // 设置搜索表单的值
            searchForm.setFieldValue('gunCode', state.searchPileCode);

            // 更新搜索参数并触发搜索
            updateSearchParams({
                ...searchParams,
                gunCode: state.searchPileCode,
                page: 1 // 重置到第一页
            });

            // 清除location.state，避免重复处理
            window.history.replaceState({}, document.title);
        }
    }, [location.state]);

  // 处理表格变化
  const handleTableChange: TableProps<Gun>['onChange'] = (pag, filters, sorter) => {
    let newParams = {
      ...searchParams,
      page: pag.current || 1,
      size: pag.pageSize || 10
    };

    // 处理排序
    if (sorter && !Array.isArray(sorter) && sorter.field) {
      newParams.sortField = sorter.field as string;
      newParams.sortOrder = sorter.order === 'ascend' ? 'asc' : 'desc';
    } else {
      delete newParams.sortField;
      delete newParams.sortOrder;
    }

      updateSearchParams(newParams);
  };

  // 搜索处理
  const handleSearch = (values: any) => {
    const newParams = {
      page: 1,
      size: pagination.pageSize,
      ...values
    };
      updateSearchParams(newParams);
  };

  // 重置搜索
  const handleReset = () => {
    searchForm.resetFields();
    const newParams = {
      page: 1,
      size: pagination.pageSize
    };
      updateSearchParams(newParams);
  };

    // 刷新数据
    const handleRefresh = () => {
        // 使用当前的搜索参数重新加载数据
        updateSearchParams({...searchParams});
        setSelectedRowKeys([]);
    };

  // 显示新建模态框
  const showCreateModal = () => {
    setIsEdit(false);
    setCurrentRecord(null);
    setModalVisible(true);
    form.resetFields();
  };

  // 处理编辑
  const handleEdit = (record: Gun) => {
    setIsEdit(true);
    setCurrentRecord(record);
    setModalVisible(true);
    form.setFieldsValue({
      ...record,
      gunNo: record.gunNo.toString()
    });
  };

    // 处理调试 - 跳转到调试页面，携带当前查询参数
    const handleDebug = (record: Gun) => {
        // 直接从URL中获取当前的查询参数，确保获取到最新的参数
        const currentUrlParams = new URLSearchParams(window.location.search);
        const queryString = currentUrlParams.toString();
        const returnUrl = queryString ? `/page/guns?${queryString}` : '/page/guns';

        console.log('调试跳转 - 当前URL参数:', queryString);
        console.log('调试跳转 - 返回URL:', returnUrl);

        // 将returnUrl作为URL参数传递
        navigate(`/page/guns/${record.gunCode}/debug?returnUrl=${encodeURIComponent(returnUrl)}`);
  };

  // 生成充电枪编码
  const handleGenerateGunCode = () => {
    const pileId = form.getFieldValue('pileId');
    const gunNo = form.getFieldValue('gunNo');
    
    if (!pileId || !gunNo) {
      showMessage.warning('请先选择充电桩和填写枪号');
      return;
    }
    
    const selectedPile = pileOptions.find(p => p.id === pileId);
    if (selectedPile) {
      const code = generateGunCode(selectedPile.pileCode, gunNo);
      form.setFieldValue('gunCode', code);
    }
  };

  // 处理表单提交
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setModalLoading(true);

      if (isEdit && currentRecord) {
        // 编辑充电枪
        const updateData: GunUpdateRequest = {
          gunName: values.gunName,
          gunNo: values.gunNo,
          gunCode: values.gunCode,
          stationId: values.stationId,
          pileId: values.pileId
        };
        await gunService.updateGun(currentRecord.id, updateData);
        showMessage.success('充电枪更新成功');
      } else {
        // 新建充电枪
        const createData: GunCreateRequest = {
          gunName: values.gunName,
          gunNo: values.gunNo,
          gunCode: values.gunCode,
          stationId: values.stationId,
          pileId: values.pileId
        };
        await gunService.createGun(createData);
        showMessage.success('充电枪创建成功');
      }

      setModalVisible(false);
      // 清空选择状态并重新加载数据
      setSelectedRowKeys([]);
      loadData();
    } catch (error: any) {
      if (error.errorFields) {
        // 表单验证错误
        return;
      }
      showMessage.error(getErrorMessage(error));
    } finally {
      setModalLoading(false);
    }
  };

  // 取消模态框
  const handleCancel = () => {
    setModalVisible(false);
    form.resetFields();
  };

  // 处理删除
  const handleDelete = async (record: Gun) => {
    try {
      console.log('开始删除充电枪:', record.gunName, 'ID:', record.id);
      await gunService.deleteGun(record.id);
      console.log('删除充电枪成功:', record.gunName);
      showMessage.success(`充电枪 "${record.gunName}" 删除成功`);
      // 清空选择状态并重新加载数据
      setSelectedRowKeys([]);
      loadData();
    } catch (error: any) {
      console.error('删除充电枪失败:', error);
      console.error('错误详情:', {
        response: error?.response,
        data: error?.response?.data,
        status: error?.response?.status,
        message: error?.message
      });
      
      const errorMessage = getErrorMessage(error);
      console.log('处理后的错误消息:', errorMessage);
      
      showMessage.error(`删除充电枪 "${record.gunName}" 失败：${errorMessage}`);
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
          <p>您确定要删除选中的 <strong style={{ color: '#ff4d4f' }}>{selectedRowKeys.length}</strong> 条充电枪吗？</p>
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
              await gunService.deleteGun(key as string);
              successCount++;
              
              // 每删除一个都更新进度提示
              if (selectedRowKeys.length > 3) {
                showMessage.loading(`正在删除... (${successCount}/${selectedRowKeys.length})`);
              }
            } catch (error: any) {
              failCount++;
              const record = dataSource.find(item => item.id === key);
              const gunName = record?.gunName || `ID: ${key}`;
              failedNames.push(gunName);
              
              // 获取详细错误信息
              const errorMessage = getErrorMessage(error);
              failedReasons.push(`${gunName}: ${errorMessage}`);
            }
          }

          // 显示删除结果
          if (failCount === 0) {
            showMessage.success(`批量删除成功，共删除 ${successCount} 条充电枪`);
          } else if (successCount === 0) {
            // 全部失败
            showMessage.error(
              `批量删除失败，所有 ${failCount} 条充电枪都删除失败。失败原因：${failedReasons.join('; ')}`
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
  };

  return (
    <div>
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: 16 
      }}>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600, color: '#262626' }}>
          充电枪管理
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
            新建充电枪
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
              <Form.Item label="充电枪名称" name="gunName" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入充电枪名称"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="充电枪编码" name="gunCode" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入充电枪编码"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="枪号" name="gunNo" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入枪号"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="所属充电站" name="stationId" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择充电站"
                  allowClear
                  showSearch
                  optionFilterProp="children"
                >
                  {stationOptions.map(station => (
                    <Select.Option key={station.id} value={station.id}>
                      {station.stationName}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col span={6}>
              <Form.Item label="所属充电桩" name="pileId" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择充电桩"
                  allowClear
                  showSearch
                  optionFilterProp="children"
                >
                  {pileOptions.map(pile => (
                    <Select.Option key={pile.id} value={pile.id}>
                      {pile.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="运行状态" name="runStatus" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择运行状态"
                  allowClear
                >
                  <Select.Option value="IDLE">空闲</Select.Option>
                  <Select.Option value="INSERTED">已插枪</Select.Option>
                  <Select.Option value="CHARGING">充电中</Select.Option>
                  <Select.Option value="CHARGE_COMPLETE">充电完成</Select.Option>
                  <Select.Option value="DISCHARGE_READY">放电准备</Select.Option>
                  <Select.Option value="DISCHARGING">放电中</Select.Option>
                  <Select.Option value="DISCHARGE_COMPLETE">放电完成</Select.Option>
                  <Select.Option value="RESERVED">预约中</Select.Option>
                  <Select.Option value="FAULT">故障</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item style={{ marginBottom: 0 }}>
                <Space>
                  <Button icon={<ReloadOutlined />} onClick={handleReset}>
                    重置
                  </Button>
                  <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
                    搜索
                  </Button>
                </Space>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Card>

      {/* 数据表格 */}
      <Card
        title="充电枪列表"
        extra={
            <Space size="small">
                <Button
                    icon={<ReloadOutlined/>}
                    type="text"
                    size="small"
                    style={{padding: '4px 8px'}}
                    title="刷新数据"
                    onClick={handleRefresh}
                />
                <Dropdown
                    menu={columnSelectorMenu}
                    placement="bottomRight"
                    trigger={['click']}
                    overlayStyle={{minWidth: 180}}
                >
                    <Button
                        icon={<TableOutlined/>}
                        type="text"
                        size="small"
                        style={{padding: '4px 8px'}}
                        title="自定义列"
                    />
                </Dropdown>
            </Space>
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

      {/* 新增/编辑充电枪Modal */}
      <Modal
        title={isEdit ? '编辑充电枪' : '新建充电枪'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={handleCancel}
        confirmLoading={modalLoading}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
        >
          {isEdit && (
            <Form.Item name="id" hidden>
              <Input />
            </Form.Item>
          )}
          
          <Form.Item
            label="充电枪名称"
            name="gunName"
            rules={[{ required: true, message: '请输入充电枪名称' }]}
          >
            <Input placeholder="请输入充电枪名称" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="所属充电站"
                name="stationId"
                rules={[{ required: true, message: '请选择充电站' }]}
              >
                <Select
                  placeholder="请选择充电站"
                  showSearch
                  allowClear
                  filterOption={(input, option) =>
                    (option?.children as unknown as string)?.toLowerCase().includes(input.toLowerCase())
                  }
                >
                  {stationOptions.map(station => (
                    <Select.Option key={station.id} value={station.id}>
                      {station.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="所属充电桩"
                name="pileId"
                rules={[{ required: true, message: '请选择充电桩' }]}
              >
                <Select
                  placeholder="请选择充电桩"
                  showSearch
                  allowClear
                  filterOption={(input, option) =>
                    (option?.children as unknown as string)?.toLowerCase().includes(input.toLowerCase())
                  }
                >
                  {pileOptions.map(pile => (
                    <Select.Option key={pile.id} value={pile.id}>
                      {pile.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="枪号"
                name="gunNo"
                rules={[{ required: true, message: '请输入枪号' }]}
              >
                <Input placeholder="请输入枪号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="充电枪编码"
                name="gunCode"
                rules={[{ required: true, message: '请输入充电枪编码' }]}
              >
                <Input
                  placeholder="请输入充电枪编码"
                  disabled={isEdit}
                  suffix={
                    <Button
                      type="link"
                      size="small"
                      onClick={handleGenerateGunCode}
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
        </Form>
      </Modal>

    </div>
  );
};

export default GunManagement;
