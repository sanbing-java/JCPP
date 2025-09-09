/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */

-- 数据库版本表
CREATE TABLE IF NOT EXISTS t_schema_version
(
    version varchar(32) not null primary key
);

-- 插入初始版本
INSERT INTO t_schema_version (version) VALUES ('1.0.0') ON CONFLICT (version) DO NOTHING;

CREATE TABLE IF NOT EXISTS t_user
(
    id               uuid                                not null
        constraint owner_pkey
            primary key,
    created_time     timestamp default CURRENT_TIMESTAMP not null,
    updated_time     timestamp,
    additional_info  jsonb,
    status           varchar(16)                         not null,
    user_name        varchar(255)                        not null,
    user_credentials jsonb                               not null,
    authority        varchar(32),
    version          int                                 default 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uni_user_name
    on t_user (user_name);

-- 为t_user表的字段添加注释
COMMENT ON COLUMN t_user.authority IS '用户权限: SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER, REFRESH_TOKEN, PRE_VERIFICATION_TOKEN';

-- 为authority字段创建索引，便于按权限查询用户
CREATE INDEX IF NOT EXISTS idx_user_authority
    on t_user (authority);

CREATE TABLE IF NOT EXISTS t_station
(
    id              uuid                                not null
        constraint station_pkey
            primary key,
    created_time    timestamp default CURRENT_TIMESTAMP not null,
    updated_time    timestamp,
    additional_info jsonb,
    station_name    varchar(255)                        not null,
    station_code    varchar(255)                        not null,
    longitude       double precision,
    latitude        double precision,
    province        varchar(255),
    city            varchar(255),
    county          varchar(255),
    address         varchar(255),
    version         int                                 default 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uni_station_code
    on t_station (station_code);

CREATE TABLE IF NOT EXISTS t_pile
(
    id              uuid                                not null
        constraint pile_pkey
            primary key,
    created_time    timestamp default CURRENT_TIMESTAMP not null,
    updated_time    timestamp,
    additional_info jsonb,
    pile_name       varchar(255)                        not null,
    pile_code       varchar(255)                        not null,
    protocol        varchar(255)                        not null,
    station_id      uuid                                not null,
    brand           varchar(255),
    model           varchar(255),
    manufacturer    varchar(255),
    type            varchar(16)                         not null,
    version         int                                 default 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uni_pile_code
    on t_pile (pile_code);


CREATE TABLE IF NOT EXISTS t_gun
(
    id                      uuid                                not null
        primary key,
    created_time            timestamp default CURRENT_TIMESTAMP not null,
    updated_time            timestamp default CURRENT_TIMESTAMP not null,
    additional_info         varchar(255),
    gun_no                  varchar(255)                        not null,
    gun_name                varchar(255)                        not null,
    gun_code                varchar(255)                        not null,
    station_id              uuid                                not null,
    pile_id                 uuid                                not null,
    version                 int                                 default 1
);

CREATE INDEX IF NOT EXISTS idx_gun_pile_id
    on t_gun (pile_id);

CREATE INDEX IF NOT EXISTS idx_gun_pile_gun_code
    on t_gun (pile_id, gun_code);



CREATE SEQUENCE IF NOT EXISTS attr_kv_version_seq cache 1;

-- 属性表：存储充电桩、充电枪的最新属性数据（如状态等）
-- 采用键值对存储结构设计
CREATE TABLE IF NOT EXISTS t_attr
(
    entity_id       uuid                                 not null,  -- 实体ID (UUID保证全局唯一)
    attr_key        varchar(255)                         not null,  -- 属性键 (字符串类型提高可读性)
    bool_v          boolean,                                        -- 布尔值
    str_v           varchar(10000000),                              -- 字符串值
    long_v          bigint,                                         -- 长整型值
    dbl_v           double precision,                               -- 双精度值
    json_v          json,                                           -- JSON值
    last_update_ts  bigint                               not null,  -- 最后更新时间戳
    version         int                  default 0    not null,  -- 版本号，用于乐观锁
    PRIMARY KEY (entity_id, attr_key)
);





