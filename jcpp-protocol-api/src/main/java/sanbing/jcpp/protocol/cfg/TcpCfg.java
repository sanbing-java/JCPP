/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.protocol.cfg;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TcpCfg {

    private String bindAddress;

    @Max(65000)
    private int bindPort;

    @Min(1)
    private int bossGroupThreadCount;

    @Min(1)
    private int workerGroupThreadCount;

    private boolean soKeepAlive;

    @Min(1)
    @Max(65500)
    private int soBacklog;

    @Min(1)
    private int soRcvbuf;

    @Min(1)
    private int soSndbuf;

    private boolean nodelay;

    @Valid
    private TcpHandlerCfg handler;

}