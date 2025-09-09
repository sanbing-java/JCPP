/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.config.ibatis.typehandlers;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;
import sanbing.jcpp.app.service.security.model.UserCredentials;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * UserCredentials 类型处理器
 * 负责 PostgreSQL JSONB 和 UserCredentials 对象之间的转换
 * 
 * @author 九筒
 */
@Slf4j
@MappedTypes({UserCredentials.class})
public class UserCredentialsTypeHandler extends BaseTypeHandler<UserCredentials> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UserCredentials parameter, JdbcType jdbcType) throws SQLException {
        if (ps == null) {
            throw new SQLException("PreparedStatement cannot be null");
        }
        
        if (parameter != null) {
            try {
                PGobject jsonObject = new PGobject();
                jsonObject.setType("jsonb");
                jsonObject.setValue(JacksonUtil.toString(parameter));
                ps.setObject(i, jsonObject);
                log.debug("Set UserCredentials parameter at index {}: failedLoginAttempts={}", i, parameter.getFailedLoginAttempts());
            } catch (Exception e) {
                log.error("Failed to serialize UserCredentials to JSONB", e);
                throw new SQLException("Failed to serialize UserCredentials", e);
            }
        } else {
            ps.setNull(i, java.sql.Types.OTHER);
        }
    }

    @Override
    public UserCredentials getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseUserCredentials(rs.getString(columnName), columnName);
    }

    @Override
    public UserCredentials getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseUserCredentials(rs.getString(columnIndex), "column_" + columnIndex);
    }

    @Override
    public UserCredentials getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseUserCredentials(cs.getString(columnIndex), "column_" + columnIndex);
    }

    /**
     * 解析 JSON 字符串为 UserCredentials 对象
     */
    private UserCredentials parseUserCredentials(String jsonString, String columnIdentifier) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            log.debug("UserCredentials JSON is null or empty for {}", columnIdentifier);
            return null;
        }

        try {
            UserCredentials userCredentials = JacksonUtil.fromString(jsonString, UserCredentials.class);
            if (userCredentials != null) {
                log.debug("Parsed UserCredentials from {}: failedLoginAttempts={}", 
                         columnIdentifier, userCredentials.getFailedLoginAttempts());
            }
            return userCredentials;
        } catch (Exception e) {
            log.error("Failed to parse UserCredentials from JSON: {} for {}", jsonString, columnIdentifier, e);
            // 返回 null 而不是抛出异常，避免查询失败
            return null;
        }
    }
}
