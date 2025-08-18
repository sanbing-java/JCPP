/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.mapper;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import sanbing.jcpp.AbstractTestBase;
import sanbing.jcpp.app.dal.config.ibatis.enums.OrderStatusEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.OrderTypeEnum;
import sanbing.jcpp.app.dal.entity.Order;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static sanbing.jcpp.app.dal.mapper.GunMapperIT.NORMAL_GUN_ID;
import static sanbing.jcpp.app.dal.mapper.PileMapperIT.NORMAL_PILE_ID;
import static sanbing.jcpp.app.dal.mapper.StationMapperIT.NORMAL_STATION_ID;
import static sanbing.jcpp.app.dal.mapper.UserMapperIT.NORMAL_USER_ID;

/**
 * @author baigod
 */
class OrderMapperIT extends AbstractTestBase {

    @Resource
    OrderMapper orderMapper;

    @Test
    void testOrderMapper() {
        orderMapper.delete(Wrappers.lambdaQuery());

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .internalOrderNo(IdUtil.getSnowflake(1, 1).nextIdStr())
                .externalOrderNo(IdUtil.getSnowflake(1, 1).nextIdStr())
                .pileOrderNo(RandomStringUtils.secure().nextNumeric(16))
                .createdTime(LocalDateTime.now())
                .additionalInfo(JacksonUtil.newObjectNode())
                .updatedTime(LocalDateTime.now())
                .cancelledTime(null)
                .status(OrderStatusEnum.IN_CHARGING)
                .type(OrderTypeEnum.CHARGE)
                .creatorId(NORMAL_USER_ID)
                .stationId(NORMAL_STATION_ID)
                .pileId(NORMAL_PILE_ID[0])
                .gunId(NORMAL_GUN_ID[0])
                .plateNo("浙A88888")
                .settlementAmount(new BigDecimal(100))
                .settlementDetails(JacksonUtil.newObjectNode())
                .electricityQuantity(new BigDecimal("100"))
                .build();

        orderMapper.insertOrUpdate(order);

        log.info("{}", orderMapper.selectById(order.getId()));

    }
}