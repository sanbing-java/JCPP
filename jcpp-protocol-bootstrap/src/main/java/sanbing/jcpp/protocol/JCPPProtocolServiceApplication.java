/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import sanbing.jcpp.infrastructure.util.annotation.AfterStartUp;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author 九筒
 */
@SpringBootApplication(scanBasePackages = {"sanbing.jcpp.protocol",
        "sanbing.jcpp.infrastructure.stats",
        "sanbing.jcpp.infrastructure.queue",
        "sanbing.jcpp.infrastructure.util"})
@EnableAsync
@EnableScheduling
@Slf4j
public class JCPPProtocolServiceApplication {

    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "protocol-service";

    private static long startTs;

    public static void main(String[] args) {
        startTs = System.currentTimeMillis();
        new SpringApplicationBuilder(JCPPProtocolServiceApplication.class).bannerMode(Banner.Mode.LOG).run(updateArguments(args));
    }

    private static String[] updateArguments(String[] args) {
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            String[] modifiedArgs = new String[args.length + 1];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = DEFAULT_SPRING_CONFIG_PARAM;
            return modifiedArgs;
        }
        return args;
    }

    @AfterStartUp(order = Ordered.LOWEST_PRECEDENCE)
    public void afterStartUp() {
        long startupTimeMs = System.currentTimeMillis() - startTs;
        log.info("Started JChargePointProtocol Protocol Service in {} seconds", TimeUnit.MILLISECONDS.toSeconds(startupTimeMs));
    }

}