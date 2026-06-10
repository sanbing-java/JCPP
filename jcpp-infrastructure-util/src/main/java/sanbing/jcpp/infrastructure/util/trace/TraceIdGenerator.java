/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.util.trace;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class TraceIdGenerator {

    //127.0.0.1
    private static String IP_16 = "7F000001";

    private static final int MAX_COUNT_INDEX = 9000;

    private static final AtomicInteger COUNT = new AtomicInteger(1000);

    static {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            if (ipAddress != null) {
                IP_16 = getIP_16(ipAddress);
            }
        } catch (Throwable ignored) {
        }
    }

    public static String generate() {
        return getTraceId(IP_16, System.currentTimeMillis(), getNextId());
    }

    private static String getIP_16(String ip) {
        String[] ips = ip.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String column : ips) {
            String hex = Integer.toHexString(Integer.parseInt(column)).toUpperCase();
            if (hex.length() == 1) {
                sb.append('0').append(hex);
            } else {
                sb.append(hex);
            }

        }
        return sb.toString();
    }

    private static String getTraceId(String ip, long timestamp, String nextId) {
        return ip + timestamp + nextId;
    }

    private static String getNextId() {

        int count = COUNT.incrementAndGet();

        if (count > 9000) {
            synchronized (TraceIdGenerator.class) {
                if (COUNT.get() > MAX_COUNT_INDEX) {
                    COUNT.set(1000);
                }
            }

            return String.valueOf(COUNT.incrementAndGet());
        } else {
            return String.valueOf(count);
        }

    }
}
