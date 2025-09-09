/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
import React, {useEffect, useMemo, useState} from 'react';
import {
    Button,
    Card,
    Cascader,
    Checkbox,
    Col,
    Dropdown,
    Form,
    Input,
    InputNumber,
    message,
    Modal,
    Popconfirm,
    Row,
    Select,
    Space,
    Table,
    Typography
} from 'antd';
import {DeleteOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, TableOutlined} from '@ant-design/icons';
import type {ColumnsType, TableProps} from 'antd/es/table';
import {formatTimestamp, generateStationCode, showMessage} from '../utils';
import {getErrorMessage} from '../services/api';
import * as stationService from '../services/stationService';
import type {Station} from '../types';
// 直接require china-division数据
const provinces = require('china-division/dist/provinces.json');
const cities = require('china-division/dist/cities.json');
const areas = require('china-division/dist/areas.json');

// 地区选项类型定义
interface RegionOption {
  label: string;
  value: string;
  children?: RegionOption[];
}

// 构建地区数据
const buildRegionData = (): RegionOption[] => {
  // 为每个城市添加区县数据
  areas.forEach((area: any) => {
    const matchCity = cities.find((city: any) => city.code === area.cityCode);
    if (matchCity) {
      matchCity.children = matchCity.children || [];
      matchCity.children.push({
        label: area.name,
        value: area.code,
      });
    }
  });

  // 为每个省份添加城市数据
  cities.forEach((city: any) => {
    const matchProvince = provinces.find((province: any) => province.code === city.provinceCode);
    if (matchProvince) {
      matchProvince.children = matchProvince.children || [];
      matchProvince.children.push({
        label: city.name,
        value: city.code,
        children: city.children,
      });
    }
  });

  // 构建最终的级联选择器数据
  return provinces.map((province: any) => ({
    label: province.name,
    value: province.code,
    children: province.children,
  }));
};

const chinaRegions = buildRegionData();

const { TextArea } = Input;
const { confirm } = Modal;

const StationManagement: React.FC = () => {
  const [dataSource, setDataSource] = useState<Station[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<Station | null>(null);
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

  const [searchParams, setSearchParams] = useState<{
    page: number;
    size: number;
    stationName?: string;
    stationCode?: string;
    province?: string;
    city?: string;
    sortField?: string;
    sortOrder?: string;
  }>({
    page: 1,
    size: 10
  });

  // 批量删除相关状态
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [batchDeleting, setBatchDeleting] = useState(false);

  // 地区筛选相关状态
  const [provinces, setProvinces] = useState<RegionOption[]>([]);
  const [cities, setCities] = useState<RegionOption[]>([]);
  const [counties, setCounties] = useState<RegionOption[]>([]);

  // 列可见性配置
  interface ColumnConfig {
    key: string;
    title: string;
    defaultVisible: boolean;
  }

  const columnConfigs: ColumnConfig[] = [
    { key: 'stationName', title: '充电站名称', defaultVisible: true },
    { key: 'stationCode', title: '充电站编码', defaultVisible: true },
    { key: 'longitude', title: '经度', defaultVisible: false },
    { key: 'latitude', title: '纬度', defaultVisible: false },
    { key: 'province', title: '省份', defaultVisible: true },
    { key: 'city', title: '城市', defaultVisible: true },
    { key: 'county', title: '区县', defaultVisible: true },
    { key: 'address', title: '详细地址', defaultVisible: false },
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
  const allColumns: ColumnsType<Station> = useMemo(() => [
    {
      title: '充电站名称',
      dataIndex: 'stationName',
      key: 'stationName',
      width: 200,
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
      title: '充电站编码',
      dataIndex: 'stationCode',
      key: 'stationCode',
      width: 150,
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
      title: '经度',
      dataIndex: 'longitude',
      key: 'longitude',
      width: 100,
      render: (value: number) => value ? value.toFixed(6) : '-',
    },
    {
      title: '纬度',
      dataIndex: 'latitude',
      key: 'latitude',
      width: 100,
      render: (value: number) => value ? value.toFixed(6) : '-',
    },
    {
      title: '省份',
      dataIndex: 'province',
      key: 'province',
      width: 100,
      render: (text: string) => text || '-',
    },
    {
      title: '城市',
      dataIndex: 'city',
      key: 'city',
      width: 100,
      render: (text: string) => text || '-',
    },
    {
      title: '区县',
      dataIndex: 'county',
      key: 'county',
      width: 100,
      render: (text: string) => text || '-',
    },
    {
      title: '详细地址',
      dataIndex: 'address',
      key: 'address',
      width: 200,
      render: (text: string) => text || '-',
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
      render: (record: Station) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除充电站"
            description={
              <div>
                <p>确定要删除充电站 <strong>{record.stationName}</strong> 吗？</p>
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
    }).filter(Boolean) as ColumnsType<Station>;
    
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
      const response = await stationService.getStations(searchParams);
      const { records, total } = response;
      setDataSource(records);
      setPagination(prev => ({
        ...prev,
        current: searchParams.page,
        pageSize: searchParams.size,
        total
      }));
    } catch (error: any) {
      const baseErrorMessage = getErrorMessage(error);
      const errorMessage = `加载充电站数据失败：${baseErrorMessage}`;
      showMessage.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // 监听搜索参数变化
  useEffect(() => {
    loadData();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  // 初始化地区数据
  useEffect(() => {
    initializeRegions();
  }, []);

  // 处理表格变化
  const handleTableChange: TableProps<Station>['onChange'] = (pag, filters, sorter) => {
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

    setSearchParams(newParams);
  };

  // 搜索处理
  const handleSearch = (values: any) => {
    const newParams = {
      page: 1,
      size: pagination.pageSize,
      ...values
    };
    setSearchParams(newParams);
  };

  // 重置搜索
  const handleReset = () => {
    searchForm.resetFields();
    // 重置地区选择状态
    setCities([]);
    setCounties([]);
    const newParams = {
      page: 1,
      size: pagination.pageSize
    };
    setSearchParams(newParams);
  };

  // 显示创建模态框
  const showCreateModal = () => {
    setIsEdit(false);
    setCurrentRecord(null);
    setModalVisible(true);
    form.resetFields();
  };

  // 处理编辑
  const handleEdit = (record: Station) => {
    setIsEdit(true);
    setCurrentRecord(record);
    setModalVisible(true);
    
    // 构建级联值
    const regionValue = buildRegionValue(record.province, record.city, record.county);
    
    form.setFieldsValue({
      ...record,
      region: regionValue,
    });
  };

  // 处理删除
  const handleDelete = async (record: Station) => {
    try {
      await stationService.deleteStation(record.id);
      showMessage.success(`充电站 "${record.stationName}" 删除成功`);
      setSelectedRowKeys([]);
      loadData();
    } catch (error: any) {
      const baseErrorMessage = getErrorMessage(error);
      const errorMessage = `删除充电站 "${record.stationName}" 失败：${baseErrorMessage}`;
      showMessage.error(errorMessage);
    }
  };

  // 批量删除
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      showMessage.warning('请先选择要删除的充电站');
      return;
    }
    
    confirm({
      title: '确认批量删除',
      content: (
        <div>
          <p>您确定要删除选中的 <strong style={{ color: '#ff4d4f' }}>{selectedRowKeys.length}</strong> 条充电站吗？</p>
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
              await stationService.deleteStation(key as string);
              successCount++;
              
              // 每删除一个都更新进度提示
              if (selectedRowKeys.length > 3) {
                message.loading(`正在删除... (${successCount}/${selectedRowKeys.length})`, 0.5);
              }
            } catch (error: any) {
              failCount++;
              const record = dataSource.find(item => item.id === key);
              const stationName = record?.stationName || `ID: ${key}`;
              failedNames.push(stationName);
              
              // 获取详细错误信息
              const errorMessage = getErrorMessage(error);
              failedReasons.push(`${stationName}: ${errorMessage}`);
            }
          }

          // 显示删除结果
          if (failCount === 0) {
            showMessage.success(`批量删除成功，共删除 ${successCount} 条充电站`);
          } else if (successCount === 0) {
            // 全部失败
            showMessage.error(
              `批量删除失败，所有 ${failCount} 条充电站都删除失败。失败原因：${failedReasons.join('; ')}`
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


  // 地区处理函数
  const initializeRegions = () => {
    // 提取所有省份
    const provinceOptions = chinaRegions.map((province: RegionOption) => ({
      label: province.label,
      value: province.value
    }));
    setProvinces(provinceOptions);
  };

  const handleProvinceChange = (provinceValue: string) => {
    if (!provinceValue) {
      setCities([]);
      setCounties([]);
      searchForm.setFieldsValue({ city: undefined, county: undefined });
      return;
    }

    const selectedProvince = chinaRegions.find((p: RegionOption) => p.value === provinceValue);
    if (selectedProvince && selectedProvince.children) {
      const cityOptions = selectedProvince.children.map((city: RegionOption) => ({
        label: city.label,
        value: city.value
      }));
      setCities(cityOptions);
      setCounties([]);
      searchForm.setFieldsValue({ city: undefined, county: undefined });
    } else {
      setCities([]);
      setCounties([]);
      searchForm.setFieldsValue({ city: undefined, county: undefined });
    }
  };

  const handleCityChange = (cityValue: string) => {
    if (!cityValue) {
      setCounties([]);
      searchForm.setFieldsValue({ county: undefined });
      return;
    }

    const provinceValue = searchForm.getFieldValue('province');
    const selectedProvince = chinaRegions.find((p: RegionOption) => p.value === provinceValue);
    if (selectedProvince && selectedProvince.children) {
      const selectedCity = selectedProvince.children.find((c: RegionOption) => c.value === cityValue);
      if (selectedCity && selectedCity.children) {
        const countyOptions = selectedCity.children.map((county: RegionOption) => ({
          label: county.label,
          value: county.value
        }));
        setCounties(countyOptions);
        searchForm.setFieldsValue({ county: undefined });
      } else {
        setCounties([]);
        searchForm.setFieldsValue({ county: undefined });
      }
    }
  };

  // 行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(newSelectedRowKeys);
    },
  };

  // 处理表单提交
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setModalLoading(true);

      if (isEdit && currentRecord) {
        await stationService.updateStation(currentRecord.id, values);
        // 使用showMessage显示成功消息
        showMessage.success(`充电站 "${values.stationName}" 更新成功`);
      } else {
        await stationService.createStation(values);
        // 使用showMessage显示成功消息
        showMessage.success(`充电站 "${values.stationName}" 创建成功`);
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
      
      const baseErrorMessage = getErrorMessage(error);
      const operation = isEdit ? '更新' : '创建';
      const stationName = form.getFieldValue('stationName') || '充电站';
      const errorMessage = `${operation}充电站 "${stationName}" 失败：${baseErrorMessage}`;
      
      // 使用showMessage显示错误消息
      showMessage.error(errorMessage);
    } finally {
      setModalLoading(false);
    }
  };

  // 取消模态框
  const handleCancel = () => {
    setModalVisible(false);
    form.resetFields();
  };

  // 生成充电站编码
  const handleGenerateStationCode = () => {
    const code = generateStationCode();
    form.setFieldValue('stationCode', code);
  };

  // 处理级联选择器变化
  const handleRegionChange = (values: string[], selectedOptions?: RegionOption[]) => {
    if (selectedOptions && selectedOptions.length > 0) {
      const province = selectedOptions[0]?.label || '';
      const city = selectedOptions[1]?.label || '';
      const county = selectedOptions[2]?.label || '';
      
      // 更新表单的隐藏字段，用于提交数据
      form.setFieldsValue({
        province,
        city,
        county,
      });
    } else {
      // 清空时重置所有地址字段
      form.setFieldsValue({
        province: '',
        city: '',
        county: '',
      });
    }
  };

  // 根据省市区数据构建级联值
  const buildRegionValue = (province?: string, city?: string, county?: string): string[] => {
    if (!province) return [];
    
    // 找到省份（支持模糊匹配，去除"省"、"市"、"自治区"等后缀）
    const normalizeProvinceName = (name: string) => {
      return name.replace(/(省|市|自治区|特别行政区)$/g, '');
    };
    
    const provinceOption = chinaRegions.find((p: RegionOption) => 
      p.label === province || 
      normalizeProvinceName(p.label) === normalizeProvinceName(province)
    );
    if (!provinceOption) return [];
    
    const result = [provinceOption.value];
    
    if (city && provinceOption.children) {
      // 找到城市（支持模糊匹配）
      const normalizeCityName = (name: string) => {
        return name.replace(/(市|区|县|自治州|地区|盟)$/g, '');
      };
      
      const cityOption = provinceOption.children.find((c: RegionOption) => 
        c.label === city || 
        normalizeCityName(c.label) === normalizeCityName(city)
      );
      if (cityOption) {
        result.push(cityOption.value);
        
        if (county && cityOption.children) {
          // 找到区县（支持模糊匹配）
          const normalizeCountyName = (name: string) => {
            return name.replace(/(区|县|市)$/g, '');
          };
          
          const countyOption = cityOption.children.find((d: RegionOption) => 
            d.label === county || 
            normalizeCountyName(d.label) === normalizeCountyName(county)
          );
          if (countyOption) {
            result.push(countyOption.value);
          }
        }
      }
    }
    
    return result;
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
          充电站管理
        </h2>
        <Space>
        <Button 
          danger 
          icon={<DeleteOutlined />} 
          onClick={handleBatchDelete}
          loading={batchDeleting}
          disabled={selectedRowKeys.length === 0}
        >
          批量删除 ({selectedRowKeys.length})
        </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={showCreateModal}>
            新建充电站
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
              <Form.Item label="充电站名称" name="stationName" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入充电站名称"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="充电站编码" name="stationCode" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入充电站编码"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="省份" name="province" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择省份"
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={provinces}
                  onChange={handleProvinceChange}
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="城市" name="city" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择城市"
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={cities}
                  onChange={handleCityChange}
                  disabled={!cities.length}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col span={6}>
              <Form.Item label="区县" name="county" style={{ marginBottom: 0 }}>
                <Select
                  placeholder="请选择区县"
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={counties}
                  disabled={!counties.length}
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="详细地址" name="address" style={{ marginBottom: 0 }}>
                <Input
                  placeholder="请输入详细地址"
                  allowClear
                />
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
        title="充电站列表"
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

      <Modal
        title={isEdit ? '编辑充电站' : '新建充电站'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={handleCancel}
        confirmLoading={modalLoading}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            longitude: null,
            latitude: null,
          }}
        >
          <Form.Item
            label="充电站名称"
            name="stationName"
            rules={[{ required: true, message: '请输入充电站名称' }]}
          >
            <Input placeholder="请输入充电站名称" />
          </Form.Item>

          <Form.Item
            label="充电站编码"
            name="stationCode"
            rules={[{ required: true, message: '请输入充电站编码' }]}
          >
            <Input
              placeholder="请输入充电站编码"
              disabled={isEdit}
              suffix={
                <Button
                  type="link"
                  size="small"
                  onClick={handleGenerateStationCode}
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

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="经度"
                name="longitude"
              >
                <InputNumber
                  placeholder="请输入经度（可选）"
                  precision={6}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="纬度"
                name="latitude"
              >
                <InputNumber
                  placeholder="请输入纬度（可选）"
                  precision={6}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="所在地区"
            name="region"
            tooltip="选择省市区，将自动填充对应字段"
          >
            <Cascader
              options={chinaRegions}
              placeholder="请选择省份/城市/区县"
              allowClear
              changeOnSelect
              showSearch
              onChange={handleRegionChange}
              displayRender={(labels) => labels.join(' / ')}
            />
          </Form.Item>

          {/* 隐藏字段，用于存储省市区数据 */}
          <Form.Item name="province" style={{ display: 'none' }}>
            <Input />
          </Form.Item>
          <Form.Item name="city" style={{ display: 'none' }}>
            <Input />
          </Form.Item>
          <Form.Item name="county" style={{ display: 'none' }}>
            <Input />
          </Form.Item>

          <Form.Item
            label="详细地址"
            name="address"
          >
            <TextArea placeholder="请输入详细地址（可选）" rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default StationManagement;
