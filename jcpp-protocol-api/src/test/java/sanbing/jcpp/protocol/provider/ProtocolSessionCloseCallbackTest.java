/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sanbing.jcpp.proto.gen.DownlinkProto.DownlinkRequestMessage;
import sanbing.jcpp.proto.gen.UplinkProto.SessionCloseReason;
import sanbing.jcpp.protocol.domain.ProtocolSession;
import sanbing.jcpp.protocol.provider.impl.DefaultProtocolSessionRegistryProvider;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会话关闭回调单元测试
 * 测试 ProtocolSession 关闭时自动清除 Caffeine 缓存功能
 *
 * @author 九筒
 */
class ProtocolSessionCloseCallbackTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private DefaultProtocolSessionRegistryProvider sessionRegistryProvider;

    @BeforeEach
    void setUp() {
        sessionRegistryProvider = new DefaultProtocolSessionRegistryProvider();
    }

    /**
     * 测试：会话关闭后应自动从缓存中移除
     */
    @Test
    void testSessionRemovedFromCacheOnClose() {
        log.info("开始测试：会话关闭后应自动从缓存中移除");

        // 1. 创建并注册会话
        TestProtocolSession session = new TestProtocolSession("testProtocol");
        UUID sessionId = session.getId();

        sessionRegistryProvider.register(session);
        log.info("会话已注册: sessionId={}", sessionId);

        // 2. 验证会话存在于缓存中
        ProtocolSession sessionFromCache = sessionRegistryProvider.get(sessionId);
        assertNotNull(sessionFromCache, "注册后会话应存在于缓存中");
        assertEquals(sessionId, sessionFromCache.getId(), "会话ID应匹配");
        log.info("验证会话存在于缓存中");

        // 3. 关闭会话
        session.close(SessionCloseReason.SESSION_CLOSE_ON_CHANNEL_INACTIVE);
        log.info("会话已关闭");

        // 4. 验证会话已从缓存中移除
        ProtocolSession sessionAfterClose = sessionRegistryProvider.get(sessionId);
        assertNull(sessionAfterClose, "关闭后会话应从缓存中移除");
        log.info("验证会话已从缓存中移除");

        // 5. 验证会话状态
        assertTrue(session.isClosed(), "会话 closed 状态应为 true");
        log.info("验证会话 closed 状态为 true");

        log.info("测试通过：会话关闭后自动从缓存中移除");
    }

    /**
     * 测试：重复关闭会话不应抛出异常，且回调只执行一次
     */
    @Test
    void testDuplicateCloseExecutesCallbackOnce() {
        log.info("开始测试：重复关闭会话回调只执行一次");

        // 使用计数器跟踪回调执行次数
        AtomicInteger callbackCount = new AtomicInteger(0);

        // 1. 创建会话并设置自定义回调
        TestProtocolSession session = new TestProtocolSession("testProtocol");
        session.setCloseCallback(id -> {
            callbackCount.incrementAndGet();
            log.info("回调执行，当前次数: {}", callbackCount.get());
        });

        // 2. 第一次关闭
        session.close(SessionCloseReason.SESSION_CLOSE_ON_CHANNEL_INACTIVE);
        assertEquals(1, callbackCount.get(), "第一次关闭后回调应执行1次");
        assertTrue(session.isClosed(), "第一次关闭后应为 closed 状态");

        // 3. 第二次关闭（应被忽略）
        assertDoesNotThrow(() -> session.close(SessionCloseReason.SESSION_CLOSE_DESTRUCTION));
        assertEquals(1, callbackCount.get(), "重复关闭后回调仍应只执行1次");
        assertTrue(session.isClosed(), "重复关闭后仍应为 closed 状态");

        // 4. 第三次关闭（应被忽略）
        assertDoesNotThrow(() -> session.close());
        assertEquals(1, callbackCount.get(), "再次重复关闭后回调仍应只执行1次");

        log.info("测试通过：重复关闭只触发一次回调");
    }

    /**
     * 测试：register 时会自动设置 closeCallback
     */
    @Test
    void testRegisterSetsCloseCallback() {
        log.info("开始测试：register 时会自动设置 closeCallback");

        // 1. 创建会话（此时 closeCallback 为 null）
        TestProtocolSession session = new TestProtocolSession("testProtocol");
        assertNull(session.getCloseCallback(), "注册前 closeCallback 应为 null");

        // 2. 注册会话
        sessionRegistryProvider.register(session);

        // 3. 验证 closeCallback 已设置
        assertNotNull(session.getCloseCallback(), "注册后 closeCallback 应已设置");
        log.info("验证 closeCallback 已设置");

        // 4. 验证关闭时能正确清除缓存
        UUID sessionId = session.getId();
        assertNotNull(sessionRegistryProvider.get(sessionId), "关闭前会话应存在");

        session.close();

        assertNull(sessionRegistryProvider.get(sessionId), "关闭后会话应被移除");
        log.info("验证关闭时正确清除缓存");

        log.info("测试通过：register 自动设置 closeCallback");
    }

    /**
     * 测试：isClosed 方法正确反映会话状态
     */
    @Test
    void testIsClosedReflectsSessionState() {
        log.info("开始测试：isClosed 方法正确反映会话状态");

        TestProtocolSession session = new TestProtocolSession("testProtocol");

        // 新建会话应未关闭
        assertFalse(session.isClosed(), "新建会话应未关闭");

        // 关闭后应为已关闭
        session.close();
        assertTrue(session.isClosed(), "关闭后应为已关闭");

        log.info("测试通过：isClosed 方法正确反映会话状态");
    }

    /**
     * 测试用的 ProtocolSession 实现
     */
    private static class TestProtocolSession extends ProtocolSession {

        public TestProtocolSession(String protocolName) {
            super(protocolName);
        }

        @Override
        public void onDownlink(DownlinkRequestMessage downlinkMsg) {
            // 测试用空实现
        }
    }
}
