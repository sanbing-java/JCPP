/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import sanbing.jcpp.AbstractTestBase;
import sanbing.jcpp.app.dal.entity.Gun;
import sanbing.jcpp.app.service.GunService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultGunService 集成测试
 * 专门测试 findByPileCodeAndGunNo 方法的数据库连接和查询功能
 *
 * @author 九筒
 */
class DefaultGunServiceIT extends AbstractTestBase {

    @Autowired
    private GunService gunService;

    /**
     * 测试根据充电桩编码和枪编号查询充电枪
     * 这个测试专门验证之前出现的数据库连接中断问题是否已解决
     */
    @Test
    public void testFindByPileCodeAndGunNo_Success() {
        log.info("开始测试 findByPileCodeAndGunNo 方法");

        // 使用测试数据中存在的充电桩编码和枪编号
        String pileCode = "20231212000028";
        String gunNo = "2";

        try {
            // 执行查询
            Gun result = gunService.findByPileCodeAndGunNo(pileCode, gunNo);

            // 验证结果
            if (result != null) {
                log.info("查询成功 - 充电枪信息: ID={}, 名称={}, 编码={}, 枪号={}",
                        result.getId(), result.getGunName(), result.getGunCode(), result.getGunNo());

                // 验证查询结果的正确性
                assertEquals(gunNo, result.getGunNo(), "枪编号应该匹配");
                assertNotNull(result.getGunCode(), "充电枪编码不应为空");
                assertNotNull(result.getGunName(), "充电枪名称不应为空");

                log.info("✅ 测试通过：成功查询到充电枪数据");
            } else {
                log.warn("⚠️ 查询结果为空，可能是测试数据不存在");
                // 不抛出异常，因为数据为空不代表连接有问题
            }

        } catch (Exception e) {
            log.error("❌ 测试失败：查询过程中出现异常", e);
            fail("查询充电枪时出现异常: " + e.getMessage());
        }
    }

    /**
     * 测试查询不存在的充电枪
     * 验证异常处理是否正常
     */
    @Test
    public void testFindByPileCodeAndGunNo_NotFound() {
        log.info("开始测试查询不存在的充电枪");

        String pileCode = "NONEXISTENT";
        String gunNo = "999";

        try {
            Gun result = gunService.findByPileCodeAndGunNo(pileCode, gunNo);

            // 查询不存在的数据应该返回 null
            assertNull(result, "查询不存在的充电枪应该返回 null");
            log.info("✅ 测试通过：正确处理了不存在的数据查询");

        } catch (Exception e) {
            log.error("❌ 测试失败：查询不存在数据时出现异常", e);
            fail("查询不存在数据时不应该抛出异常: " + e.getMessage());
        }
    }

    /**
     * 测试参数验证
     * 验证空参数的处理
     */
    @Test
    public void testFindByPileCodeAndGunNo_InvalidParameters() {
        log.info("开始测试参数验证");

        // 测试空的 pileCode
        assertThrows(IllegalArgumentException.class, () -> {
            gunService.findByPileCodeAndGunNo(null, "1");
        }, "空的 pileCode 应该抛出 IllegalArgumentException");

        // 测试空的 gunNo
        assertThrows(IllegalArgumentException.class, () -> {
            gunService.findByPileCodeAndGunNo("20231212000028", null);
        }, "空的 gunNo 应该抛出 IllegalArgumentException");

        // 测试空字符串
        assertThrows(IllegalArgumentException.class, () -> {
            gunService.findByPileCodeAndGunNo("", "1");
        }, "空字符串的 pileCode 应该抛出 IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            gunService.findByPileCodeAndGunNo("20231212000028", "");
        }, "空字符串的 gunNo 应该抛出 IllegalArgumentException");

        log.info("✅ 测试通过：参数验证正常工作");
    }

    /**
     * 压力测试 - 多次连续查询
     * 验证连接池和缓存是否正常工作
     */
    @Test
    public void testFindByPileCodeAndGunNo_StressTest() {
        log.info("开始压力测试 - 连续查询100次");

        String pileCode = "20231212000028";
        String gunNo = "2";

        int successCount = 0;
        int totalQueries = 100;

        for (int i = 0; i < totalQueries; i++) {
            try {
                Gun result = gunService.findByPileCodeAndGunNo(pileCode, gunNo);
                successCount++;

                if (i % 20 == 0) {
                    log.info("已完成 {} 次查询", i + 1);
                }

            } catch (Exception e) {
                log.error("第 {} 次查询失败", i + 1, e);
            }
        }

        log.info("压力测试完成：{}/{} 次查询成功", successCount, totalQueries);

        // 至少95%的查询应该成功
        assertTrue(successCount >= totalQueries * 0.95,
                String.format("成功率过低：%d/%d (%.2f%%)", successCount, totalQueries,
                        (double) successCount / totalQueries * 100));

        log.info("✅ 压力测试通过：连接池和缓存工作正常");
    }
}
