/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

/**
 * @author 九筒
 */
@Configuration
public class DownlinkRestTemplateConfiguration {

    @Bean("downlinkRestTemplate")
    public RestTemplate downlinkRestTemplate() {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.of(3, ChronoUnit.SECONDS))
                .readTimeout(Duration.of(3, ChronoUnit.SECONDS))
                .build();
        restTemplate.setMessageConverters(Collections.singletonList(new ProtobufHttpMessageConverter()));
        return restTemplate;
    }
}