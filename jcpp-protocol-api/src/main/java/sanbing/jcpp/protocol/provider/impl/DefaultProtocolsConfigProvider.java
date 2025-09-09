/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.provider.impl;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import sanbing.jcpp.infrastructure.util.config.ConstraintValidator;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.protocol.cfg.ProtocolCfg;
import sanbing.jcpp.protocol.provider.ProtocolsConfigProvider;

import java.util.Map;

@Slf4j
@Setter
@Service
@ConfigurationProperties("service")
public class DefaultProtocolsConfigProvider implements ProtocolsConfigProvider {

    private Map<String, ProtocolCfg> protocols;

    @Override
    public ProtocolCfg loadConfig(String protocol) {

        ProtocolCfg protocolCfg = protocols.get(protocol);

        log.info("load {}'s configuration: \n{}", protocol, JacksonUtil.toPrettyString(protocolCfg));

        ConstraintValidator.validateFields(protocolCfg, "'" + protocol + "' configuration is invalid:");

        return protocolCfg;
    }
}