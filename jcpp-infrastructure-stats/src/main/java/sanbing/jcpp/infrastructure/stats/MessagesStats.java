/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.stats;

public interface MessagesStats {
    default void incrementTotal() {
        incrementTotal(1);
    }

    void incrementTotal(int amount);

    default void incrementSuccessful() {
        incrementSuccessful(1);
    }

    void incrementSuccessful(int amount);

    default void incrementFailed() {
        incrementFailed(1);
    }

    void incrementFailed(int amount);

    int getTotal();

    int getSuccessful();

    int getFailed();

    void reset();
}
