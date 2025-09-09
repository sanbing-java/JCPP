/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.infrastructure.util;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author 九筒
 */
public class JCPPHashUtil {
    public static HashFunction forName(String name) {
        return switch (name) {
            case "murmur3_32" -> Hashing.murmur3_32_fixed();
            case "murmur3_128" -> Hashing.murmur3_128();
            case "sha256" -> Hashing.sha256();
            default -> throw new IllegalArgumentException("Can't find hash function with name " + name);
        };
    }

    public static int hash(HashFunction hashFunction, String key) {
        return hashFunction.hashString(key, StandardCharsets.UTF_8).asInt();
    }

    public static int hash(HashFunction hashFunction, UUID key) {
        return hashFunction.hashString(key.toString(), StandardCharsets.UTF_8).asInt();
    }

}