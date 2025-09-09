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
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.UUID;

import static sanbing.jcpp.app.dal.mapper.PileMapperIT.NORMAL_PILE_ID;
import static sanbing.jcpp.app.dal.mapper.StationMapperIT.NORMAL_STATION_ID;


/**
 * @author 九筒
 */
class GunMapperIT extends AbstractTestBase {
    static final UUID[] NORMAL_GUN_ID = new UUID[]{
            UUID.fromString("8f1ffb5b-e536-4f2b-8cd0-31f7d0348a44"),
            UUID.fromString("ae256617-b747-4110-b27a-00773e03bed1"),
            UUID.fromString("d15dbb29-ea2f-4094-b448-dff853e9275f"),
            UUID.fromString("b4a2de24-d7ff-4828-a0d8-2429a6253f9c"),
            UUID.fromString("f505f7e2-9e1c-4251-8f7f-9a8eae84372a"),
            UUID.fromString("0c5bab7b-786b-4e05-ab26-618c3f5a6086"),
            UUID.fromString("2db4ad92-e353-4ac2-a2b0-942cb778eca6"),
            UUID.fromString("203833e7-0a44-4f1c-935e-cd43e6dbbf46"),
            UUID.fromString("3f3a61e9-de55-4177-9b4e-3a1d8c529890"),
            UUID.fromString("cf1a8970-5aa9-4636-a76e-d6bcf98b4a07")
    };

    @Resource
    GunMapper gunMapper;

    @Test
    void curdTest() {
        gunMapper.delete(Wrappers.lambdaQuery());

        for (int i = 0; i < NORMAL_PILE_ID.length; i++) {
            UUID pileId = NORMAL_PILE_ID[i];
            UUID gunId = NORMAL_GUN_ID[i];

            Gun gun = Gun.builder()
                    .id(gunId)
                    .createdTime(LocalDateTime.now())
                    .additionalInfo(JacksonUtil.newObjectNode())
                    .gunNo("02")
                    .gunName(String.format("三丙家的%d号充电桩", i + 1) + "的2号枪")
                    .gunCode("202312120000" + new DecimalFormat("00").format(i + 1) + "-02")
                    .stationId(NORMAL_STATION_ID)
                    .pileId(pileId)
                    .build();

            gunMapper.insertOrUpdate(gun);

            log.info("{}", gunMapper.selectById(gunId));
        }

    }
}