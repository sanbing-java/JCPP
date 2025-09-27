/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.event.TransactionalEventListener;
import sanbing.jcpp.app.adapter.response.GunWithStatusResponse;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.app.dal.mapper.GunMapper;
import sanbing.jcpp.app.dal.repository.GunRepository;
import sanbing.jcpp.app.service.cache.gun.GunCacheEvictEvent;
import sanbing.jcpp.app.service.cache.gun.GunCacheKey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static sanbing.jcpp.infrastructure.util.validation.Validator.validateId;
import static sanbing.jcpp.infrastructure.util.validation.Validator.validateString;

/**
 * 充电枪数据访问实现
 * 
 * @author 九筒
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class GunRepositoryImpl extends CachedVersionedEntityRepository<GunCacheKey, Gun, GunCacheEvictEvent> implements GunRepository {

    private final GunMapper gunMapper;

    @TransactionalEventListener(classes = GunCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(GunCacheEvictEvent event) {
        // 如果修改或删除充电枪，需要在这里消费删除事件
        List<GunCacheKey> toEvict = new ArrayList<>(3);
        
        // 基于gunId的缓存key
        if (event.getGunId() != null) {
            toEvict.add(new GunCacheKey(event.getGunId()));
        }

        // 基于pileCode+gunNo的缓存key
        if (event.getPileCode() != null && event.getGunNo() != null) {
            toEvict.add(new GunCacheKey(event.getPileCode(), event.getGunNo()));
        }

        // 基于gunCode的缓存key
        if (event.getGunCode() != null) {
            toEvict.add(new GunCacheKey(event.getGunCode()));
        }
        
        cache.evict(toEvict);
    }


    @Override
    public Gun findByPileCodeAndGunNo(String pileCode, String gunNo) {
        validateString(pileCode, code -> "无效的桩编号: " + pileCode);
        validateString(gunNo, no -> "无效的枪编号: " + gunNo);

        return cache.get(new GunCacheKey(pileCode, gunNo),
                () -> gunMapper.selectByPileCodeAndGunNo(pileCode, gunNo));
    }

    @Override
    public Gun findByGunCode(String gunCode) {
        validateString(gunCode, code -> "无效的枪编号: " + gunCode);

        return cache.get(new GunCacheKey(gunCode),
                () -> gunMapper.selectByGunCode(gunCode));
    }

    @Override
    public Gun findById(UUID gunId) {
        validateId(gunId, id -> "无效的充电枪ID: " + gunId);
        
        return cache.get(new GunCacheKey(gunId),
                () -> gunMapper.selectById(gunId));
    }

    @Override
    public GunWithStatusResponse findGunWithStatusByCode(String gunCode) {
        validateString(gunCode, code -> "无效的枪编号: " + gunCode);

        // 这个方法不使用缓存，因为它包含状态信息，需要实时查询
        return gunMapper.selectGunWithStatusByCode(gunCode);
    }
}
