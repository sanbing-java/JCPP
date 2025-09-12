/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.adapter.request.GunCreateRequest;
import sanbing.jcpp.app.adapter.request.GunQueryRequest;
import sanbing.jcpp.app.adapter.request.GunUpdateRequest;
import sanbing.jcpp.app.adapter.response.GunWithStatusResponse;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.dal.config.ibatis.enums.GunRunStatusEnum;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.app.dal.mapper.GunMapper;
import sanbing.jcpp.app.dal.repository.GunRepository;
import sanbing.jcpp.app.data.kv.AttrKeyEnum;
import sanbing.jcpp.app.data.kv.AttributeKvEntry;
import sanbing.jcpp.app.data.kv.BaseAttributeKvEntry;
import sanbing.jcpp.app.data.kv.StringDataEntry;
import sanbing.jcpp.app.service.AttributeService;
import sanbing.jcpp.app.service.GunService;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.UplinkProto.GunRunStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultGunService implements GunService {

    private final GunMapper gunMapper;
    private final GunRepository gunRepository;
    private final AttributeService attributeService;

    @Override
    public Gun createGun(GunCreateRequest request) {
        // 检查充电枪编码是否已存在
        LambdaQueryWrapper<Gun> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Gun::getGunCode, request.getGunCode());
        if (gunMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("充电枪编码已存在");
        }

        Gun gun = Gun.builder()
                .id(UUID.randomUUID())
                .createdTime(LocalDateTime.now())
                .gunName(request.getGunName())
                .gunNo(request.getGunNo())
                .gunCode(request.getGunCode())
                .stationId(request.getStationId())
                .pileId(request.getPileId())
                .additionalInfo(JacksonUtil.newObjectNode())
                .version(0)
                .build();

        gunMapper.insert(gun);
        return gun;
    }

    @Override
    public Gun findById(UUID id) {
        return gunRepository.findById(id);
    }

    @Override
    public Gun updateGun(UUID id, GunUpdateRequest request) {
        Gun existingGun = findById(id);
        if (existingGun == null) {
            throw new RuntimeException("充电枪不存在，更新失败");
        }
        
        Gun updatedGun = Gun.builder()
                .id(existingGun.getId())
                .createdTime(existingGun.getCreatedTime())
                .updatedTime(LocalDateTime.now()) // 更新时设置更新时间
                .gunName(request.getGunName())
                .gunNo(request.getGunNo()) // 允许修改枪号
                .gunCode(request.getGunCode()) // 允许修改编码
                .stationId(UUID.fromString(request.getStationId())) // 允许修改所属充电站
                .pileId(UUID.fromString(request.getPileId())) // 允许修改所属充电桩
                .additionalInfo(existingGun.getAdditionalInfo())
                .version(existingGun.getVersion())
                .build();

        gunMapper.updateById(updatedGun);
        return updatedGun;
    }

    @Override
    public void deleteGun(UUID id) {
        Gun gun = findById(id);
        if (gun == null) {
            throw new RuntimeException("充电枪不存在，删除失败");
        }
        
        int affectedRows = gunMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new RuntimeException("删除充电枪失败，可能已被其他操作删除");
        }
    }
    @Override
    public PageResponse<GunWithStatusResponse> queryGunsWithStatus(GunQueryRequest request) {
        Page<GunWithStatusResponse> page = new Page<>(request.getPage(), request.getSize());
        
        // 使用MyBatis XML配置查询，避免魔法值错误
        IPage<GunWithStatusResponse> result = gunMapper.selectGunWithStatusPage(page, request);
        
        return PageResponse.<GunWithStatusResponse>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .totalPages((int) result.getPages())
                .page(request.getPage())
                .size(request.getSize())
                .build();
    }

    @Override
    public Gun findByPileCodeAndGunCode(String pileCode, String gunCode) {
        return gunRepository.findByPileCodeAndGunCode(pileCode, gunCode);
    }
    
    @Override
    public String findGunStatus(UUID gunId) {
        ListenableFuture<Optional<AttributeKvEntry>> attribute = attributeService.find(gunId, AttrKeyEnum.STATUS.getCode());

        try {
            Optional<AttributeKvEntry> result = attribute.get();
            if (result.isPresent()) {
                AttributeKvEntry entry = result.get();
                Optional<String> strValue = entry.getStrValue();
                return strValue.orElse(null);
            }
            return null;
        } catch (Exception e) {
            log.error("获取充枪状态失败: gunId={}", gunId, e);
            return null;
        }
    }
    
    @Override
    public void saveGunStatusChange(UUID gunId, String status, Long ts) {
        try {
            long currentTime = ts != null ? ts : System.currentTimeMillis();
            AttributeKvEntry gunStatusAttr = new BaseAttributeKvEntry(
                new StringDataEntry(AttrKeyEnum.GUN_RUN_STATUS.getCode(), status), 
                currentTime
            );
            
            attributeService.save(gunId, gunStatusAttr);
            
            log.info("充电枪状态已保存: gunId={}, status={}, ts={}", gunId, status, ts);
        } catch (Exception e) {
            log.error("保存充电枪状态失败: gunId={}, status={}", gunId, status, e);
        }
    }

    @Override
    public boolean handleGunRunStatus(String pileCode, String gunCode, GunRunStatus protoStatus, long ts) {
        log.info("处理充电枪状态上报: 桩编码={}, 枪编码={}, 状态={}", pileCode, gunCode, protoStatus);
        
        // 将Proto状态转换为数据库枚举
        GunRunStatusEnum dbStatus = convertProtoStatusToDbStatus(protoStatus);
        
        if (dbStatus != null) {
            // 获取充电枪信息（使用缓存）
            Gun gun = findByPileCodeAndGunCode(pileCode, gunCode);
            if (gun != null) {
                // 检查状态是否真的发生了变化，避免重复保存
                String currentStatus = findGunStatus(gun.getId());
                if (dbStatus.name().equals(currentStatus)) {
                    log.debug("充电枪状态未发生变化，跳过更新: 桩编码={}, 枪编码={}, 状态={}", pileCode, gunCode, dbStatus);
                    return false;
                }
                
                // 保存充电枪状态到属性表
                saveGunStatusChange(gun.getId(), dbStatus.name(), ts);
                
                log.info("充电枪状态更新成功: 桩编码={}, 枪编码={}, 原状态={}, 新状态={}", 
                        pileCode, gunCode, currentStatus, dbStatus);
                
                // 根据充电枪状态判断是否需要更新充电桩状态
                return shouldUpdatePileStatus(dbStatus);
            } else {
                log.warn("未找到充电枪: 桩编码={}, 枪编码={}", pileCode, gunCode);
            }
        } else {
            log.warn("未知的充电枪状态: {}, 跳过更新", protoStatus);
        }
        
        return false;
    }

    /**
     * 将Proto状态转换为数据库枚举状态
     */
    private GunRunStatusEnum convertProtoStatusToDbStatus(GunRunStatus protoStatus) {
        return switch (protoStatus) {
            case IDLE -> GunRunStatusEnum.IDLE;
            case INSERTED -> GunRunStatusEnum.INSERTED;
            case CHARGING -> GunRunStatusEnum.CHARGING;
            case CHARGE_COMPLETE -> GunRunStatusEnum.CHARGE_COMPLETE;
            case DISCHARGE_READY -> GunRunStatusEnum.DISCHARGE_READY;
            case DISCHARGING -> GunRunStatusEnum.DISCHARGING;
            case DISCHARGE_COMPLETE -> GunRunStatusEnum.DISCHARGE_COMPLETE;
            case RESERVED -> GunRunStatusEnum.RESERVED;
            case FAULT -> GunRunStatusEnum.FAULT;
            default -> null; // 未知状态不更新
        };
    }

    /**
     * 根据充电枪状态判断是否需要更新充电桩状态
     */
    private boolean shouldUpdatePileStatus(GunRunStatusEnum gunStatus) {
        return switch (gunStatus) {
            case CHARGING, DISCHARGING -> true; // 充电或放电时可能需要更新桩状态为在线
            case FAULT -> true; // 故障时可能需要更新桩状态
            default -> false; // 其他状态不需要更新桩状态
        };
    }

}
