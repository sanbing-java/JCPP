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
import sanbing.jcpp.app.adapter.request.PileCreateRequest;
import sanbing.jcpp.app.adapter.request.PileQueryRequest;
import sanbing.jcpp.app.adapter.request.PileUpdateRequest;
import sanbing.jcpp.app.adapter.response.PageResponse;
import sanbing.jcpp.app.adapter.response.PileOptionResponse;
import sanbing.jcpp.app.adapter.response.PileWithStatusResponse;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileStatusEnum;
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.dal.mapper.GunMapper;
import sanbing.jcpp.app.dal.mapper.PileMapper;
import sanbing.jcpp.app.dal.repository.PileRepository;
import sanbing.jcpp.app.data.kv.*;
import sanbing.jcpp.app.exception.JCPPErrorCode;
import sanbing.jcpp.app.exception.JCPPException;
import sanbing.jcpp.app.service.AttributeService;
import sanbing.jcpp.app.service.PileService;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultPileService implements PileService {

    private final PileMapper pileMapper;
    private final PileRepository pileRepository;
    private final AttributeService attributeService;
    private final GunMapper gunMapper;


    @Override
    public Pile createPile(PileCreateRequest request) {
        // 检查充电桩编码是否已存在
        LambdaQueryWrapper<Pile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pile::getPileCode, request.getPileCode());
        if (pileMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("充电桩编码已存在");
        }

        Pile pile = Pile.builder()
                .id(UUID.randomUUID())
                .createdTime(LocalDateTime.now())
                .pileName(request.getPileName())
                .pileCode(request.getPileCode())
                .protocol(request.getProtocol())
                .stationId(request.getStationId())
                .brand(request.getBrand())
                .model(request.getModel())
                .manufacturer(request.getManufacturer())
                .type(request.getType())
                .additionalInfo(JacksonUtil.newObjectNode())
                .version(0)
                .build();

        pileMapper.insert(pile);
        return pile;
    }

    @Override
    public Pile findById(UUID id) {
        return pileMapper.selectById(id);
    }

    @Override
    public Pile findByPileCode(String pileCode) {
        return pileRepository.findPileByCode(pileCode);
    }

    @Override
    public Pile updatePile(UUID id, PileUpdateRequest request) throws JCPPException {
        Pile existingPile = findById(id);
        if (existingPile == null) {
            throw new JCPPException("充电桩不存在", JCPPErrorCode.ITEM_NOT_FOUND);
        }

        Pile updatedPile = Pile.builder()
                .id(existingPile.getId())
                .createdTime(existingPile.getCreatedTime())
                .updatedTime(LocalDateTime.now()) // 更新时设置更新时间
                .pileName(request.getPileName())
                .pileCode(existingPile.getPileCode()) // 编码不允许修改
                .protocol(request.getProtocol())
                .stationId(existingPile.getStationId()) // 所属充电站不允许修改
                .brand(request.getBrand())
                .model(request.getModel())
                .manufacturer(request.getManufacturer())
                .type(request.getType())
                .additionalInfo(existingPile.getAdditionalInfo())
                .version(existingPile.getVersion())
                .build();

        pileMapper.updateById(updatedPile);
        return updatedPile;
    }

    @Override
    public void deletePile(UUID id) throws JCPPException {
        // 检查充电桩是否存在
        Pile pile = findById(id);
        if (pile == null) {
            throw new JCPPException("充电桩不存在", JCPPErrorCode.ITEM_NOT_FOUND);
        }

        // 检查充电桩下是否存在充电枪
        long gunCount = gunMapper.countByPileId(id);
        if (gunCount > 0) {
            throw new JCPPException(
                String.format("无法删除充电桩[%s]，该充电桩下还有 %d 支充电枪，请先删除所有充电枪", 
                    pile.getPileName(), gunCount), 
                JCPPErrorCode.VERSION_CONFLICT);
        }

        // 执行删除
        int affectedRows = pileMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new JCPPException("删除充电桩失败，可能已被其他操作删除", JCPPErrorCode.VERSION_CONFLICT);
        }
    }


    @Override
    public PageResponse<PileWithStatusResponse> queryPilesWithStatus(PileQueryRequest request) {
        // 添加详细的分页调试日志
        Page<PileWithStatusResponse> page = new Page<>(request.getPage(), request.getSize());

        // 使用AttrKeyEnum消除魔法值，提高代码可维护性
        IPage<PileWithStatusResponse> result = pileMapper.selectPileWithStatusPage(
                page,
                request,
                AttrKeyEnum.STATUS,
                AttrKeyEnum.CONNECTED_AT,
                AttrKeyEnum.DISCONNECTED_AT,
                AttrKeyEnum.LAST_ACTIVE_TIME
        );

        return PageResponse.<PileWithStatusResponse>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .totalPages((int) result.getPages())
                .page(request.getPage())
                .size(request.getSize())
                .build();
    }

    @Override
    public void updatePileStatus(UUID pileId, PileStatusEnum status) {
        try {
            // 获取现有充电桩信息
            Pile existingPile = findById(pileId);
            if (existingPile == null) {
                log.warn("充电桩不存在，无法更新状态: ID={}, 状态={}", pileId, status);
                return;
            }

            // 检查状态是否真的发生了变化，避免重复保存
            String currentStatus = findPileStatus(pileId);
            if (status.name().equals(currentStatus)) {
                log.debug("充电桩状态未发生变化，跳过更新: ID={}, 状态={}", pileId, status);
                return;
            }

            long currentTime = System.currentTimeMillis();

            // 更新状态属性
            AttributeKvEntry statusAttr = new BaseAttributeKvEntry(
                    new StringDataEntry(AttrKeyEnum.STATUS.getCode(), status.name()),
                    currentTime
            );
            attributeService.save(pileId, statusAttr);

            log.info("充电桩状态更新成功: ID={}, 桩编码={}, 原状态={}, 新状态={}",
                    pileId, existingPile.getPileCode(), currentStatus, status);
        } catch (Exception e) {
            log.error("更新充电桩状态失败: ID={}, 状态={}", pileId, status, e);
            throw new RuntimeException("更新充电桩状态失败", e);
        }
    }

    @Override
    public void updatePileStatusByCode(String pileCode, PileStatusEnum status) {
        try {
            // 根据编码查找充电桩
            Pile pile = pileRepository.findPileByCode(pileCode);
            if (pile == null) {
                log.warn("根据编码未找到充电桩: pileCode={}", pileCode);
                return;
            }

            // 调用基于ID的更新方法
            updatePileStatus(pile.getId(), status);
        } catch (Exception e) {
            log.error("根据编码更新充电桩状态失败: pileCode={}, 状态={}", pileCode, status, e);
            throw new RuntimeException("根据编码更新充电桩状态失败", e);
        }
    }

    @Override
    public List<Pile> findAll() {
        return pileMapper.selectList(null);
    }

    @Override
    public List<Pile> findPilesWithPagination(int offset, int limit) {
        return pileMapper.selectPage(new Page<>(offset / limit + 1, limit), null).getRecords();
    }

    @Override
    public String findPileStatus(UUID pileId) {
        // 直接从数据库查询，避免异步复杂性
        ListenableFuture<Optional<AttributeKvEntry>> attribute = attributeService.find(pileId, AttrKeyEnum.STATUS.getCode());
        try {
            Optional<AttributeKvEntry> result = attribute.get();
            if (result.isPresent()) {
                AttributeKvEntry entry = result.get();
                Optional<String> strValue = entry.getStrValue();
                return strValue.orElse(null);
            }
            return null;
        } catch (Exception e) {
            log.error("获取充电桩状态失败: pileId={}", pileId, e);
            return null;
        }
    }

    @Override
    public ListenableFuture<AttributesSaveResult> handlePileLogin(UUID pileId) {
        long currentTime = System.currentTimeMillis();
        List<AttributeKvEntry> attributesToUpdate = new ArrayList<>();

        // 1. 更新STATUS为ONLINE
        attributesToUpdate.add(new BaseAttributeKvEntry(
                new StringDataEntry(AttrKeyEnum.STATUS.getCode(), PileStatusEnum.ONLINE.name()),
                currentTime));

        // 2. 更新CONNECTED_AT为当前系统时间
        attributesToUpdate.add(new BaseAttributeKvEntry(
                new LongDataEntry(AttrKeyEnum.CONNECTED_AT.getCode(), currentTime),
                currentTime));

        // 3. 更新LAST_ACTIVE_TIME为当前系统时间
        attributesToUpdate.add(new BaseAttributeKvEntry(
                new LongDataEntry(AttrKeyEnum.LAST_ACTIVE_TIME.getCode(), currentTime),
                currentTime));

        // 批量保存属性
        return attributeService.save(pileId, attributesToUpdate);
    }

    @Override
    public ListenableFuture<AttributesSaveResult> handlePileHeartbeat(UUID pileId) {
        long currentTime = System.currentTimeMillis();
        List<AttributeKvEntry> attributesToUpdate = new ArrayList<>();

        // 1. 更新STATUS为ONLINE
        attributesToUpdate.add(new BaseAttributeKvEntry(
                new StringDataEntry(AttrKeyEnum.STATUS.getCode(), PileStatusEnum.ONLINE.name()),
                currentTime));

        // 2. 更新LAST_ACTIVE_TIME为当前系统时间
        attributesToUpdate.add(new BaseAttributeKvEntry(
                new LongDataEntry(AttrKeyEnum.LAST_ACTIVE_TIME.getCode(), currentTime),
                currentTime));

        // 批量保存属性
        return attributeService.save(pileId, attributesToUpdate);
    }

    @Override
    public void handlePileSessionClose(String pileCode) {
            Pile pile = pileRepository.findPileByCode(pileCode);
            if (pile == null) {
                log.warn("充电桩会话关闭处理失败，未找到充电桩: pileCode={}", pileCode);
                return;
            }

            long currentTime = System.currentTimeMillis();
            List<AttributeKvEntry> attributesToUpdate = new ArrayList<>();

            // 1. 更新STATUS为OFFLINE
            attributesToUpdate.add(new BaseAttributeKvEntry(
                    new StringDataEntry(AttrKeyEnum.STATUS.getCode(), PileStatusEnum.OFFLINE.name()),
                    currentTime));

            // 2. 更新DISCONNECTED_AT为当前系统时间
            attributesToUpdate.add(new BaseAttributeKvEntry(
                    new LongDataEntry(AttrKeyEnum.DISCONNECTED_AT.getCode(), currentTime),
                    currentTime));

            // 批量保存属性
            attributeService.save(pile.getId(), attributesToUpdate);

            log.info("充电桩会话关闭，设置为离线状态: 桩编码={}", pileCode);
    }

    @Override
    public List<PileOptionResponse> getPileOptions() {
        List<Pile> piles = pileMapper.selectList(null);
        return piles.stream()
                .map(pile -> PileOptionResponse.builder()
                        .id(pile.getId())
                        .label(pile.getPileName() + " (" + pile.getPileCode() + ")")
                        .pileName(pile.getPileName())
                        .pileCode(pile.getPileCode())
                        .stationId(pile.getStationId())
                        .build())
                .collect(Collectors.toList());
    }
}
