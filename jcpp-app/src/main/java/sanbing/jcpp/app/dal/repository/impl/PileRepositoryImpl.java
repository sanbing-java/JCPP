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
import sanbing.jcpp.app.dal.entity.Pile;
import sanbing.jcpp.app.dal.mapper.PileMapper;
import sanbing.jcpp.app.dal.repository.PileRepository;
import sanbing.jcpp.app.service.cache.pile.PileCacheEvictEvent;
import sanbing.jcpp.app.service.cache.pile.PileCacheKey;

import java.util.ArrayList;
import java.util.List;

import static sanbing.jcpp.infrastructure.util.validation.Validator.validateString;

/**
 * @author 九筒
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class PileRepositoryImpl extends CachedVersionedEntityRepository<PileCacheKey, Pile, PileCacheEvictEvent> implements PileRepository {

    private final PileMapper pileMapper;

    @TransactionalEventListener(classes = PileCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(PileCacheEvictEvent event) {
        // 如果修改或删除充电桩，需要在这里消费删除事件
        List<PileCacheKey> toEvict = new ArrayList<>(3);
        toEvict.add(new PileCacheKey(event.getPileId()));
        toEvict.add(new PileCacheKey(event.getPileCode()));
        cache.evict(toEvict);
    }

    @Override
    public Pile findPileByCode(String pileCode) {
        validateString(pileCode, code -> "无效的桩编号" + pileCode);
        return cache.get(new PileCacheKey(pileCode),
                () -> pileMapper.selectByCode(pileCode));
    }
}