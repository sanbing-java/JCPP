/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import sanbing.jcpp.AbstractTestBase;
import sanbing.jcpp.app.dal.entity.Station;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.time.LocalDateTime;
import java.util.UUID;



/**
 * @author 九筒
 */
class StationMapperIT extends AbstractTestBase {
    static final UUID NORMAL_STATION_ID = UUID.fromString("07d80c81-fe99-4a1f-a6aa-dc4d798b5626");

    @Resource
    StationMapper stationMapper;

    @Test
    void curdTest() {
        stationMapper.delete(Wrappers.lambdaQuery());

        Station station = Station.builder()
                .id(NORMAL_STATION_ID)
                .createdTime(LocalDateTime.now())
                .additionalInfo(JacksonUtil.newObjectNode())
                .stationName("三丙家专属充电站")
                .stationCode("S20241001001")
                .longitude(120.107936F)
                .latitude(30.267014F)
                .province("浙江省")
                .city("杭州市")
                .county("西湖区")
                .address("西溪路552-1号")
                .build();

        stationMapper.insertOrUpdate(station);

        log.info("{}", stationMapper.selectById(NORMAL_STATION_ID));
    }
}