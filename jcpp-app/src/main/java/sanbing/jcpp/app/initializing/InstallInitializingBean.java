/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.initializing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import sanbing.jcpp.app.dal.config.ibatis.enums.AuthorityEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileTypeEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.UserStatusEnum;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.dal.entity.Station;
import sanbing.jcpp.app.dal.entity.User;
import sanbing.jcpp.app.dal.mapper.GunMapper;
import sanbing.jcpp.app.dal.mapper.PileMapper;
import sanbing.jcpp.app.dal.mapper.StationMapper;
import sanbing.jcpp.app.dal.mapper.UserMapper;
import sanbing.jcpp.app.data.InstallModeEnum;
import sanbing.jcpp.app.data.kv.*;
import sanbing.jcpp.app.service.AttributeService;
import sanbing.jcpp.app.service.security.model.UserCredentials;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数据库安装组件
 * 在Spring容器初始化时执行数据库操作
 * 如果失败会阻止应用启动，确保数据库环境正确
 * 
 * @author 九筒
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(0)
public class InstallInitializingBean implements InitializingBean {
    
    /**
     * 安装模式
     * init - 初始化数据库并加载演示数据
     * upgrade - 升级数据库
     * disabled - 不执行任何操作
     */
    @Value("${install.mode:disabled}")
    private String mode;
    
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    
    // Mappers for data insertion
    private final UserMapper userMapper;
    private final StationMapper stationMapper;
    private final PileMapper pileMapper;
    private final GunMapper gunMapper;
    
    // Services for demo data
    private final AttributeService attributeService;
    
    @Override
    @Transactional
    public void afterPropertiesSet() throws Exception {
        if (isDisabled()) {
            log.info("数据库安装功能已禁用，跳过安装操作");
            return;
        }
        
        try {
            performInstallation();
            log.info("数据库安装操作完成");
        } catch (Exception e) {
            log.error("数据库安装操作失败，应用启动终止", e);
            // 抛出异常阻止Spring容器启动
            throw new RuntimeException("数据库初始化失败，应用无法启动", e);
        }
    }
    
    private boolean isDisabled() {
        return "disabled".equals(mode) || mode == null || mode.isEmpty();
    }
    
    private void performInstallation() {
        InstallModeEnum installMode = InstallModeEnum.fromMode(mode);
        log.info("开始执行数据库安装操作，模式: {}", installMode.getDescription());
        
        switch (installMode) {
            case INIT:
                doInitDatabase();
                break;
            case UPGRADE:
                doUpgradeDatabase();
                break;
            case DISABLED:
                log.info("数据库安装功能已禁用");
                break;
            default:
                log.warn("未知的安装模式: {}", mode);
        }
    }

    /**
     * 实际执行数据库初始化的内部方法
     * 包括创建表结构和加载演示数据
     */
    private void doInitDatabase() {
        log.info("开始初始化数据库...");
        
        try {
            // 1. 执行数据库架构初始化脚本
            String schemaScript = loadResourceFile("sql/schema-init.sql");
            log.info("执行数据库架构初始化脚本");
            jdbcTemplate.execute(schemaScript);
            log.info("数据库架构初始化完成");
            
            // 2. 加载演示数据
            log.info("开始加载演示数据...");
            doLoadDemoData();
            log.info("演示数据加载完成");
            
            log.info("数据库初始化完成");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }
    
    /**
     * 升级数据库
     */
    private void doUpgradeDatabase() {
        log.info("开始升级数据库...");
        // TODO: 实现升级逻辑
        log.info("数据库升级完成");
    }
    
    /**
     * 加载演示数据的内部方法
     */
    private void doLoadDemoData() {
        log.info("开始加载演示数据...");
        
        try {
            // 创建系统管理员账号
            createAdminUserIfNotExists();
            
            // 创建5个演示充电站
            createDemoStationsIfNotExists();

            log.info("演示数据加载完成");
        } catch (Exception e) {
            log.error("加载演示数据失败", e);
            throw new RuntimeException("加载演示数据失败", e);
        }
    }
    
    private String loadResourceFile(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        byte[] binaryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(binaryData, StandardCharsets.UTF_8);
    }
    
    private void createAdminUserIfNotExists() {
        try {
            // 检查是否已存在同名用户（大小写不敏感）
            int userCount = userMapper.countByUserName("sanbing");
            
            if (userCount == 0) {
                // 使用固定的UUID作为管理员用户ID
                UUID adminUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                ObjectNode additionalInfo = JacksonUtil.newObjectNode();
                
                // 创建UserCredentials对象
                UserCredentials credentials = new UserCredentials();
                // 使用BCrypt加密密码
                String encodedPassword = passwordEncoder.encode("sanbing@123456");
                credentials.setPassword(encodedPassword);
                credentials.setEnabled(true);
                credentials.setFailedLoginAttempts(0);
                
                User adminUser = User.builder()
                        .id(adminUserId)
                        .createdTime(LocalDateTime.now())
                        .additionalInfo(additionalInfo)
                        .status(UserStatusEnum.ENABLE)
                        .userName("sanbing")
                        .userCredentials(credentials)
                        .authority(AuthorityEnum.SYS_ADMIN)  // 设置为系统管理员权限
                        .version(1)
                        .build();
                
                userMapper.insert(adminUser);
                log.info("创建系统管理员账号: {}, 权限: {}", adminUser.getUserName(), adminUser.getAuthority());
            } else {
                log.info("系统管理员账号已存在，跳过创建");
            }
        } catch (Exception e) {
            log.error("创建系统管理员账号失败", e);
            throw e;
        }
    }

    /**
     * 创建5个演示充电站，每个站配置不同数量的充电桩和枪
     */
    private void createDemoStationsIfNotExists() {
        String[][] stationData = {
            {"07d80c81-fe99-4a1f-a6aa-dc4d798b5626", "三丙家专属充电站", "S20241001001", "120.1079330444336", "30.267013549804688", "浙江省", "杭州市", "西湖区", "西溪路552-1号"},
            {"17d80c81-fe99-4a1f-a6aa-dc4d798b5627", "西湖区政府充电站", "S20241001002", "120.1279330444336", "30.277013549804688", "浙江省", "杭州市", "西湖区", "文三路168号"},
            {"27d80c81-fe99-4a1f-a6aa-dc4d798b5628", "杭州大厦充电站", "S20241001003", "120.1679330444336", "30.257013549804688", "浙江省", "杭州市", "下城区", "延安路385号"},
            {"37d80c81-fe99-4a1f-a6aa-dc4d798b5629", "钱江新城充电站", "S20241001004", "120.1879330444336", "30.247013549804688", "浙江省", "杭州市", "江干区", "富春路701号"},
            {"47d80c81-fe99-4a1f-a6aa-dc4d798b562a", "滨江高新充电站", "S20241001005", "120.1979330444336", "30.207013549804688", "浙江省", "杭州市", "滨江区", "江南大道588号"}
        };
        
        for (int i = 0; i < stationData.length; i++) {
            Station station = createStationIfNotExists(stationData[i]);
            
            // 为每个充电站创建充电桩和充电枪
            // 站1: 6桩10枪, 站2: 6桩10枪, 站3: 6桩10枪, 站4: 6桩10枪, 站5: 6桩10枪 = 30桩50枪
            createDemoPilesAndGunsForStation(station, i + 1);
        }
    }
    
    private Station createStationIfNotExists(String[] data) {
        try {
            UUID stationId = UUID.fromString(data[0]);
            Station existingStation = stationMapper.selectById(stationId);
            
            if (existingStation == null) {
                ObjectNode additionalInfo = JacksonUtil.newObjectNode();
                
                Station station = Station.builder()
                        .id(stationId)
                        .createdTime(LocalDateTime.now())
                        .additionalInfo(additionalInfo)
                        .stationName(data[1])
                        .stationCode(data[2])
                        .longitude(Float.parseFloat(data[3]))
                        .latitude(Float.parseFloat(data[4]))
                        .province(data[5])
                        .city(data[6])
                        .county(data[7])
                        .address(data[8])
                        .version(1)
                        .build();
                
                stationMapper.insert(station);
                log.info("创建演示电站: {}", station.getStationName());
                return station;
            } else {
                log.info("演示电站已存在，跳过创建: {}", data[1]);
                return existingStation;
            }
        } catch (Exception e) {
            log.error("创建演示电站失败: {}", data[1], e);
            throw e;
        }
    }
    
    /**
     * 为每个充电站创建充电桩和充电枪
     * 总计: 5站 x 6桩 = 30桩, 每桩1-2枪 = 50枪
     */
    private void createDemoPilesAndGunsForStation(Station station, int stationIndex) {
        // 每个充电站创建6个充电桩
        for (int pileIndex = 1; pileIndex <= 6; pileIndex++) {
            // 计算全局桩号：(站序号-1) * 6 + 桩序号，从20231212000001开始
            int globalPileNumber = (stationIndex - 1) * 6 + pileIndex;
            Pile pile = createDemoPileForStation(station, stationIndex, pileIndex, globalPileNumber);
            
            // 为充电桩创建充电枪
            // 前4个充电桩每个2枪，后2个充电桩每个1枪，这样每站正好10枪
            int gunsPerPile = (pileIndex <= 4) ? 2 : 1;
            createDemoGunsForPile(pile, station, stationIndex, pileIndex, gunsPerPile);
        }
    }
    
    private Pile createDemoPileForStation(Station station, int stationIndex, int pileIndex, int globalPileNumber) {
        // 生成唯一的UUID
        String uuidString = String.format("%08d-0000-4000-8000-%012d", 
            stationIndex * 1000 + pileIndex, stationIndex * 1000000L + pileIndex);
        UUID pileId = UUID.fromString(uuidString);
        
        Pile existingPile = pileMapper.selectById(pileId);
        if (existingPile != null) {
            log.info("充电桩已存在，跳过创建: {}", existingPile.getPileName());
            return existingPile;
        }
        
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        
        // 充电桩品牌和型号多样化
        String[] brands = {"星星", "特来电", "云快充", "国家电网", "南方电网", "蔚来"};
        String[] models = {"10A", "20A", "30A", "40A", "60A", "120A"};
        String brand = brands[((stationIndex - 1) * 6 + pileIndex - 1) % brands.length];
        String model = models[((stationIndex - 1) * 6 + pileIndex - 1) % models.length];
        
        // 交流桩和直流桩混合
        PileTypeEnum pileType = (pileIndex % 3 == 0) ? PileTypeEnum.DC : PileTypeEnum.AC;
        
        // 生成桩编号：从20231212000001开始递增
        String pileCode = String.format("20231212%06d", globalPileNumber);
        
        Pile pile = Pile.builder()
                .id(pileId)
                .createdTime(LocalDateTime.now())
                .additionalInfo(additionalInfo)
                .pileName(String.format("%s-%d号充电桩", station.getStationName(), pileIndex))
                .pileCode(pileCode)
                .protocol("yunkuaichongV150")
                .stationId(station.getId())
                .brand(brand)
                .model(model)
                .manufacturer(brand)
                .type(pileType)
                .version(1)
                .build();
        
        pileMapper.insert(pile);
        log.info("创建演示充电桩: {}", pile.getPileName());
        
        // 为新创建的充电桩插入演示属性（模拟在线/离线状态）
        loadDemoPileAttributes(pileId, stationIndex, pileIndex);
        
        return pile;
    }
    
    private void createDemoGunsForPile(Pile pile, Station station, int stationIndex, int pileIndex, int gunCount) {
        for (int gunIndex = 1; gunIndex <= gunCount; gunIndex++) {
            // 生成唯一的UUID
            String uuidString = String.format("%08d-1111-4000-8000-%012d", 
                stationIndex * 10000 + pileIndex * 10 + gunIndex, 
                stationIndex * 10000000L + pileIndex * 100000L + gunIndex);
            UUID gunId = UUID.fromString(uuidString);
            
            Gun existingGun = gunMapper.selectById(gunId);
            if (existingGun != null) {
                log.info("充电枪已存在，跳过创建: {}", existingGun.getGunName());
                continue;
            }
            
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            
            Gun gun = Gun.builder()
                    .id(gunId)
                    .createdTime(LocalDateTime.now())
                    .additionalInfo(additionalInfo)
                    .gunNo(String.format("%02d", gunIndex))
                    .gunName(String.format("%s-%d号枪", pile.getPileName(), gunIndex))
                    .gunCode(String.format("%s-%02d", pile.getPileCode(), gunIndex))
                    .stationId(station.getId())
                    .pileId(pile.getId())
                    .version(1)
                    .build();
            
            gunMapper.insert(gun);
            log.info("创建演示充电枪: {}", gun.getGunName());
            
            // 为新创建的充电枪插入演示属性（模拟不同运行状态）
            loadDemoGunAttributes(gunId, stationIndex, pileIndex, gunIndex);
        }
    }
    
    
    /**
     * 为充电桩加载演示属性，模拟在线/离线状态
     */
    private void loadDemoPileAttributes(UUID pileId, int stationIndex, int pileIndex) {
        long currentTime = System.currentTimeMillis();
        
        // 模拟80%在线率，20%离线率
        boolean isOnline = ((stationIndex + pileIndex) % 5) != 0; // 80%在线
        String status = isOnline ? "ONLINE" : "OFFLINE";
        
        // 插入状态属性
        AttributeKvEntry statusAttr = new BaseAttributeKvEntry(
            new StringDataEntry(AttrKeyEnum.STATUS.getCode(), status), 
            currentTime
        );
        attributeService.save(pileId, statusAttr);

        if (isOnline) {
            // 在线桩设置连接时间
            AttributeKvEntry connectedAtAttr = new BaseAttributeKvEntry(
                new LongDataEntry(AttrKeyEnum.CONNECTED_AT.getCode(), currentTime - (3600000L * (pileIndex % 12))),
                currentTime
            );
            attributeService.save(pileId, connectedAtAttr);
        } else {
            // 离线桩设置断开时间
            AttributeKvEntry disconnectedAtAttr = new BaseAttributeKvEntry(
                new LongDataEntry(AttrKeyEnum.DISCONNECTED_AT.getCode(), currentTime - (1800000L * (pileIndex % 6))),
                currentTime
            );
            attributeService.save(pileId, disconnectedAtAttr);
        }

        log.info("为充电桩 {} 设置演示状态属性: {}", pileId, status);
    }
    
    /**
     * 为充电枪加载演示属性，模拟多种运行状态
     */
    private void loadDemoGunAttributes(UUID gunId, int stationIndex, int pileIndex, int gunIndex) {
        long currentTime = System.currentTimeMillis();
        
        // 模拟九种充电枪运行状态的分布
        String[] gunStatuses = {
            "IDLE",              // 空闲 - 30%
            "IDLE", 
            "IDLE",
            "CHARGING",          // 充电中 - 25%
            "CHARGING",
            "INSERTED",          // 已插枪 - 15%
            "CHARGE_COMPLETE",   // 充电完成 - 10%
            "FAULT",             // 故障 - 8%
            "RESERVED",          // 预约 - 5%
            "DISCHARGING",       // 放电中 - 3%
            "DISCHARGE_READY",   // 放电准备 - 2%
            "DISCHARGE_COMPLETE" // 放电完成 - 2%
        };
        
        // 基于索引选择状态，确保有良好的分布
        int statusIndex = ((stationIndex - 1) * 20 + (pileIndex - 1) * 3 + (gunIndex - 1)) % gunStatuses.length;
        String gunStatus = gunStatuses[statusIndex];
        
        // 插入枪运行状态属性
        AttributeKvEntry statusAttr = new BaseAttributeKvEntry(
            new StringDataEntry(AttrKeyEnum.GUN_RUN_STATUS.getCode(), gunStatus), 
            currentTime
        );
        attributeService.save(gunId, statusAttr);
        
        // 根据状态设置额外属性
        if ("CHARGING".equals(gunStatus) || "DISCHARGING".equals(gunStatus)) {
            // 充电中或放电中的枪设置功率
            double power = 10.0 + (statusIndex % 8) * 5.0; // 10-45kW
            AttributeKvEntry powerAttr = new BaseAttributeKvEntry(
                new DoubleDataEntry("chargingPower", power), 
                currentTime
            );
            attributeService.save(gunId, powerAttr);
        }
        
        if ("FAULT".equals(gunStatus)) {
            // 故障枪设置故障代码
            String[] faultCodes = {"E001", "E002", "E003", "E004", "E005"};
            String faultCode = faultCodes[statusIndex % faultCodes.length];
            AttributeKvEntry faultAttr = new BaseAttributeKvEntry(
                new StringDataEntry("faultCode", faultCode), 
                currentTime
            );
            attributeService.save(gunId, faultAttr);
        }
        
        log.info("为充电枪 {} 设置演示状态属性: {}", gunId, gunStatus);
    }
}
