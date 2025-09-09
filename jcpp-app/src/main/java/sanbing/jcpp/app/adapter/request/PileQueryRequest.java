/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileStatusEnum;
import sanbing.jcpp.app.dal.config.ibatis.enums.PileTypeEnum;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class PileQueryRequest extends PageRequest {
    
    private String pileName;
    
    private String pileCode;
    
    private String protocol;
    
    private UUID stationId;
    
    private String brand;
    
    private String model;
    
    private String manufacturer;
    
    private PileTypeEnum type;
    
    private PileStatusEnum status;
}
