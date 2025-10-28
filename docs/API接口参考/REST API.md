# REST API

<cite>
**本文档引用的文件**   
- [UserController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/UserController.java)
- [StationController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/StationController.java)
- [PileController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/PileController.java)
- [GunController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/GunController.java)
- [ProtocolController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/ProtocolController.java)
- [RpcController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/RpcController.java)
- [BaseController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/BaseController.java)
- [ApiResponse.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/response/ApiResponse.java)
- [LoginResponse.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/response/LoginResponse.java)
- [ErrorCode.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/response/ErrorCode.java)
- [JCPPErrorCode.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/exception/JCPPErrorCode.java)
- [PageRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/PageRequest.java)
- [StationCreateRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/StationCreateRequest.java)
- [PileCreateRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/PileCreateRequest.java)
- [GunCreateRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/GunCreateRequest.java)
- [RpcRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/RpcRequest.java)
- [StartChargeDTO.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/dto/StartChargeDTO.java)
- [SecurityConfiguration.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/service/security/SecurityConfiguration.java)
- [JCPPErrorResponseHandler.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/exception/JCPPErrorResponseHandler.java)
</cite>

## 目录

1. [简介](#简介)
2. [认证机制](#认证机制)
3. [通用响应格式](#通用响应格式)
4. [分页查询参数](#分页查询参数)
5. [全局异常处理](#全局异常处理)
6. [错误码列表](#错误码列表)
7. [用户认证API](#用户认证api)
8. [设备管理API](#设备管理api)
9. [协议交互API](#协议交互api)
10. [远程过程调用API](#远程过程调用api)
11. [curl命令示例](#curl命令示例)

## 简介

本文档为JChargePointProtocol系统提供全面的REST API文档，涵盖用户认证、设备管理、协议交互和远程过程调用等核心功能。所有API端点均采用统一的响应格式和错误处理机制，确保接口的一致性和易用性。

**本文档引用的文件**

- [StationController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/StationController.java)
- [PileController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/PileController.java)
- [GunController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/GunController.java)
- [ProtocolController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/ProtocolController.java)
- [RpcController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/RpcController.java)

## 认证机制

系统采用JWT（JSON Web Token）进行身份认证和授权。客户端在成功登录后会获得一个JWT令牌，后续请求需要在请求头中携带该令牌。

### 认证方式

支持以下两种方式传递JWT令牌：

1. **Authorization请求头**（推荐）
   ```
   Authorization: Bearer <JWT_TOKEN>
   ```

2. **自定义请求头**
   ```
   X-Authorization: <JWT_TOKEN>
   ```

### 认证流程

1. 用户通过`/api/auth/login`端点进行登录，提供用户名和密码
2. 服务器验证凭据，成功后返回包含JWT令牌的响应
3. 客户端在后续所有请求的请求头中包含JWT令牌
4. 服务器验证令牌的有效性，决定是否授予访问权限

令牌过期后，客户端需要使用刷新令牌获取新的访问令牌。

**本文档引用的文件**

- [SecurityConfiguration.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/service/security/SecurityConfiguration.java)
- [LoginResponse.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/response/LoginResponse.java)

## 通用响应格式

所有API响应均采用统一的`ApiResponse`格式，确保客户端能够一致地处理成功和错误响应。

### 响应结构

```json
{
  "success": true,
  "errorCode": "SUCCESS",
  "message": "操作成功",
  "data": {},
  "timestamp": 1700000000000
}
```

### 字段说明

- **success**: 布尔值，表示请求是否成功
- **errorCode**: 错误码，成功时为"SUCCESS"，失败时为具体的错误码
- **message**: 响应消息，描述操作结果
- **data**: 响应数据，包含实际的业务数据
- **timestamp**: 时间戳，记录响应生成的时间

### 成功响应示例

```json
{
  "success": true,
  "errorCode": "SUCCESS",
  "message": "查询成功",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "stationName": "测试充电站",
    "stationCode": "ST001"
  },
  "timestamp": 1700000000000
}
```

### 错误响应示例

```json
{
  "success": false,
  "errorCode": "UNAUTHORIZED",
  "message": "用户未认证",
  "timestamp": 1700000000000
}
```

**本文档引用的文件**

- [ApiResponse.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/response/ApiResponse.java)

## 分页查询参数

大多数查询接口支持分页功能，通过`PageRequest`类定义分页参数。

### 分页参数

| 参数名       | 类型      | 必填 | 默认值  | 说明                    |
|-----------|---------|----|------|-----------------------|
| page      | Integer | 否  | 1    | 页码，从1开始               |
| size      | Integer | 否  | 10   | 每页大小                  |
| sortField | String  | 否  | -    | 排序字段                  |
| sortOrder | String  | 否  | desc | 排序方向：asc(升序)或desc(降序) |
| search    | String  | 否  | -    | 搜索关键词                 |

### 示例

获取第2页的充电站列表，每页20条记录，按创建时间降序排列：

```
GET /api/stations?page=2&size=20&sortField=createTime&sortOrder=desc
```

**本文档引用的文件**

- [PageRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/PageRequest.java)

## 全局异常处理

系统通过`BaseController`类提供统一的异常处理机制，确保所有异常都能被妥善处理并返回标准化的错误响应。

### 异常处理流程

1. 捕获控制器中的各种异常
2. 将异常转换为`JCPPException`
3. 通过`JCPPErrorResponseHandler`生成标准化的错误响应
4. 返回给客户端

### 支持的异常类型

- `Exception`: 通用异常
- `JCPPException`: JCPP自定义异常
- `MethodArgumentNotValidException`: 参数校验异常

**本文档引用的文件**

- [BaseController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/BaseController.java)
- [JCPPErrorResponseHandler.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/exception/JCPPErrorResponseHandler.java)

## 错误码列表

系统定义了统一的错误码体系，便于客户端识别和处理不同类型的错误。

### 通用错误码

| 错误码              | 说明         |
|------------------|------------|
| SUCCESS          | 操作成功       |
| SYSTEM_ERROR     | 系统异常，请稍后重试 |
| BUSINESS_ERROR   | 业务处理失败     |
| VALIDATION_ERROR | 参数校验失败     |
| BINDING_ERROR    | 数据绑定异常     |
| ILLEGAL_ARGUMENT | 参数错误       |
| ILLEGAL_STATE    | 状态错误       |

### 认证授权相关

| 错误码             | 说明            |
|-----------------|---------------|
| UNAUTHORIZED    | 用户未认证         |
| AUTH_FAILED     | 用户名或密码错误      |
| JWT_AUTH_FAILED | JWT Token认证失败 |
| FORBIDDEN       | 权限不足          |

### 资源相关

| 错误码       | 说明       |
|-----------|----------|
| NOT_FOUND | 请求的资源不存在 |
| CONFLICT  | 资源冲突     |

### 业务特定错误码

| 错误码                 | 说明       |
|---------------------|----------|
| PILE_CODE_EXISTS    | 充电桩编码已存在 |
| STATION_NAME_EXISTS | 充电站名称已存在 |
| GUN_CODE_EXISTS     | 充电枪编号已存在 |
| PILE_NOT_FOUND      | 充电桩不存在   |
| STATION_NOT_FOUND   | 充电站不存在   |
| GUN_NOT_FOUND       | 充电枪不存在   |

**本文档引用的文件**

- [ErrorCode.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/response/ErrorCode.java)
- [JCPPErrorCode.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/exception/JCPPErrorCode.java)

## 用户认证API

提供用户登录和获取用户信息的接口。

### 登录

- **HTTP方法**: POST
- **URL路径**: `/api/auth/login`
- **请求头**: 无特殊要求
- **请求体**:
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **响应体**:
  ```json
  {
    "success": true,
    "errorCode": "SUCCESS",
    "message": "操作成功",
    "data": {
      "token": "string",
      "refreshToken": "string",
      "tokenType": "Bearer",
      "user": {
        "id": "string",
        "username": "string",
        "status": "ENABLE"
      }
    },
    "timestamp": 1700000000000
  }
  ```

### 获取用户信息

- **HTTP方法**: GET
- **URL路径**: `/api/user/info`
- **请求头**: `Authorization: Bearer <JWT>`
- **响应体**:
  ```json
  {
    "success": true,
    "errorCode": "SUCCESS",
    "message": "操作成功",
    "data": {
      "id": "string",
      "username": "string",
      "status": "ENABLE"
    },
    "timestamp": 1700000000000
  }
  ```

**本文档引用的文件**

- [UserController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/UserController.java)
- [LoginResponse.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/response/LoginResponse.java)

## 设备管理API

提供对充电站、充电桩和充电枪的增删改查操作。

### 充电站管理

#### 查询充电站列表

- **HTTP方法**: GET
- **URL路径**: `/api/stations`
- **请求头**: `Authorization: Bearer <JWT>`
- **查询参数**: 继承自`PageRequest`
- **响应体**:
  ```json
  {
    "success": true,
    "errorCode": "SUCCESS",
    "message": "查询成功",
    "data": {
      "content": [
        {
          "id": "uuid",
          "stationName": "string",
          "stationCode": "string",
          "longitude": 120.123,
          "latitude": 30.456,
          "province": "string",
          "city": "string",
          "county": "string",
          "address": "string"
        }
      ],
      "totalElements": 1,
      "totalPages": 1,
      "page": 1,
      "size": 10
    },
    "timestamp": 1700000000000
  }
  ```

#### 创建充电站

- **HTTP方法**: POST
- **URL路径**: `/api/stations`
- **请求头**: `Authorization: Bearer <JWT>`
- **请求体**: `StationCreateRequest`
  ```json
  {
    "stationName": "string",
    "stationCode": "string",
    "longitude": 120.123,
    "latitude": 30.456,
    "province": "string",
    "city": "string",
    "county": "string",
    "address": "string"
  }
  ```
- **字段说明**:
    - stationName: 充电站名称，必填，不能为空
    - stationCode: 充电站编码，必填，不能为空
    - longitude: 经度，浮点数
    - latitude: 纬度，浮点数
    - province: 省份
    - city: 城市
    - county: 区县
    - address: 详细地址

**本文档引用的文件**

- [StationController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/StationController.java)
- [StationCreateRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/StationCreateRequest.java)

#### 更新充电站

- **HTTP方法**: PUT
- **URL路径**: `/api/stations/{id}`
- **请求头**: `Authorization: Bearer <JWT>`
- **路径参数**: id - 充电站ID (UUID)
- **请求体**: `StationUpdateRequest`
  ```json
  {
    "stationName": "string",
    "longitude": 120.123,
    "latitude": 30.456,
    "province": "string",
    "city": "string",
    "county": "string",
    "address": "string"
  }
  ```

#### 删除充电站

- **HTTP方法**: DELETE
- **URL路径**: `/api/stations/{id}`
- **请求头**: `Authorization: Bearer <JWT>`
- **路径参数**: id - 充电站ID (UUID)

### 充电桩管理

#### 创建充电桩

- **HTTP方法**: POST
- **URL路径**: `/api/piles`
- **请求头**: `Authorization: Bearer <JWT>`
- **请求体**: `PileCreateRequest`
  ```json
  {
    "pileName": "string",
    "pileCode": "string",
    "protocol": "string",
    "stationId": "uuid",
    "brand": "string",
    "model": "string",
    "manufacturer": "string",
    "type": "DC"
  }
  ```
- **字段说明**:
    - pileName: 充电桩名称，必填，不能为空
    - pileCode: 充电桩编码，必填，不能为空
    - protocol: 协议，必填，不能为空
    - stationId: 所属充电站ID，必填，不能为空
    - brand: 品牌
    - model: 型号
    - manufacturer: 制造商
    - type: 类型，默认为DC（直流）

**本文档引用的文件**

- [PileController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/PileController.java)
- [PileCreateRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/PileCreateRequest.java)

#### 查询充电桩列表

- **HTTP方法**: GET
- **URL路径**: `/api/piles`
- **请求头**: `Authorization: Bearer <JWT>`
- **查询参数**: 继承自`PageRequest`
- **响应体**:
  ```json
  {
    "success": true,
    "errorCode": "SUCCESS",
    "message": "查询成功",
    "data": {
      "content": [
        {
          "id": "uuid",
          "pileName": "string",
          "pileCode": "string",
          "protocol": "string",
          "stationId": "uuid",
          "brand": "string",
          "model": "string",
          "manufacturer": "string",
          "type": "DC",
          "status": "string"
        }
      ],
      "totalElements": 1,
      "totalPages": 1,
      "page": 1,
      "size": 10
    },
    "timestamp": 1700000000000
  }
  ```

### 充电枪管理

#### 创建充电枪

- **HTTP方法**: POST
- **URL路径**: `/api/guns`
- **请求头**: `Authorization: Bearer <JWT>`
- **请求体**: `GunCreateRequest`
  ```json
  {
    "gunName": "string",
    "gunNo": "string",
    "gunCode": "string",
    "stationId": "uuid",
    "pileId": "uuid"
  }
  ```
- **字段说明**:
    - gunName: 充电枪名称，必填，不能为空
    - gunNo: 充电枪编号，必填，不能为空
    - gunCode: 充电枪编码，必填，不能为空
    - stationId: 所属充电站ID，必填，不能为空
    - pileId: 所属充电桩ID，必填，不能为空

**本文档引用的文件**

- [GunController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/GunController.java)
- [GunCreateRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/GunCreateRequest.java)

## 协议交互API

提供协议相关的查询接口。

### 获取支持的协议列表

- **HTTP方法**: GET
- **URL路径**: `/api/protocols/supported`
- **请求头**: `Authorization: Bearer <JWT>`
- **响应体**:
  ```json
  {
    "success": true,
    "errorCode": "SUCCESS",
    "message": "查询成功",
    "data": [
      {
        "value": "LVNENG_V340",
        "label": "绿能V3.4.0"
      },
      {
        "value": "YUNKUAICHONG_V150",
        "label": "云快充V1.5.0"
      }
    ],
    "timestamp": 1700000000000
  }
  ```

**本文档引用的文件**

- [ProtocolController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/ProtocolController.java)

## 远程过程调用API

提供通用化的充电桩下行指令接口。

### 单向RPC

- **HTTP方法**: POST
- **URL路径**: `/api/rpc/oneway`
- **请求头**: `Authorization: Bearer <JWT>`
- **请求体**: `RpcRequest`
  ```json
  {
    "method": "string",
    "parameter": {},
    "timeoutMs": 10000
  }
  ```
- **字段说明**:
    - method: RPC方法名，必填，不能为空
    - parameter: 方法参数，JSON格式，必填，不能为空
    - timeoutMs: 超时时间（毫秒），仅用于双向RPC

**本文档引用的文件**

- [RpcController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/RpcController.java)
- [RpcRequest.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/request/RpcRequest.java)

#### 支持的方法

| 方法名                      | 说明      | 参数结构                              |
|--------------------------|---------|-----------------------------------|
| startCharge              | 启动充电    | `StartChargeDTO`                  |
| stopCharge               | 停止充电    | `StopChargeDTO`                   |
| restartPile              | 重启充电桩   | `RestartPileDTO`                  |
| setPricing               | 设置计费策略  | `SetPricingDTO`                   |
| setQrcode                | 设置二维码   | `SetQrcodeRequest`                |
| otaRequest               | OTA升级   | `OtaRequest`                      |
| offlineCardBalanceUpdate | 离线卡余额更新 | `OfflineCardBalanceUpdateRequest` |
| offlineCardSync          | 离线卡同步   | `OfflineCardSyncRequest`          |
| offlineCardClear         | 离线卡清除   | `OfflineCardClearRequest`         |
| offlineCardQuery         | 离线卡查询   | `OfflineCardQueryRequest`         |
| timeSync                 | 时间同步    | `TimeSyncDTO`                     |

### 启动充电示例

```json
{
  "method": "startCharge",
  "parameter": {
    "pileCode": "P001",
    "gunNo": "G01",
    "limitYuan": 100.00,
    "orderNo": "ORDER_001",
    "logicalCardNo": "LOGICAL_001",
    "physicalCardNo": "PHYSICAL_001",
    "parallelNo": "PARALLEL_001"
  }
}
```

**本文档引用的文件**

- [StartChargeDTO.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/dto/StartChargeDTO.java)

## curl命令示例

以下是一些常用的curl命令示例，演示如何使用API。

### 登录

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'
```

### 查询电站列表

```bash
curl -X GET "http://localhost:8080/api/stations?page=1&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 下发启动充电指令

```bash
curl -X POST "http://localhost:8080/api/rpc/oneway" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "method": "startCharge",
    "parameter": {
      "pileCode": "P001",
      "gunNo": "G01",
      "limitYuan": 100.00,
      "orderNo": "ORDER_001"
    }
  }'
```

**本文档引用的文件**

- [UserController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/UserController.java)
- [StationController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/StationController.java)
- [RpcController.java](file://jcpp-app/src/main/java/sanbing/jcpp/app/adapter/controller/RpcController.java)