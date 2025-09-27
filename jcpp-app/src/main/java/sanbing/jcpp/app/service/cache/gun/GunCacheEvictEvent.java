/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.cache.gun;

import lombok.Data;

import java.util.UUID;

@Data
public class GunCacheEvictEvent {

    private UUID gunId;
    private String pileCode;
    private String gunNo;
    private String gunCode;
}
