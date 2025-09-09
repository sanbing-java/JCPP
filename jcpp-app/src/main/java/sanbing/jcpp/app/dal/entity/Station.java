/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sanbing.jcpp.infrastructure.cache.HasVersion;
import sanbing.jcpp.infrastructure.util.validation.NoXss;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@TableName("t_station")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Station implements Serializable, HasVersion {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private JsonNode additionalInfo;

    @NoXss
    private String stationName;

    @NoXss
    private String stationCode;

    private Float longitude;

    private Float latitude;

    @NoXss
    private String province;

    @NoXss
    private String city;

    @NoXss
    private String county;

    @NoXss
    private String address;

    private Integer version;

}
