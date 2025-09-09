/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sanbing.jcpp.app.adapter.response.ProtocolOption;
import sanbing.jcpp.app.service.ProtocolService;
import sanbing.jcpp.protocol.enums.SupportedProtocols;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 协议服务实现
 * 
 * @author 九筒
 * @since 2024-12-22
 */
@Slf4j
@Service
public class DefaultProtocolService implements ProtocolService {

    @Override
    public List<ProtocolOption> getSupportedProtocols() {
        // 直接从SupportedProtocols获取，无需缓存
        return SupportedProtocols.getAllProtocols()
                .stream()
                .map(ProtocolOption::fromProtocolInfo)
                .collect(Collectors.toList());
    }
}