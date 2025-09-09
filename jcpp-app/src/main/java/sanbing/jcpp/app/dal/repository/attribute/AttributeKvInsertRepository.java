/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.dal.repository.attribute;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import sanbing.jcpp.app.dal.entity.Attribute;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Repository
public class AttributeKvInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));
    private static final String EMPTY_STR = "";

    @Value("${sql.remove_null_chars:true}")
    private boolean removeNullChars;

    @Resource
    protected JdbcTemplate jdbcTemplate;

    @Resource
    protected TransactionTemplate transactionTemplate;

    private static final String BATCH_UPDATE = "UPDATE t_attr SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v = cast(? AS json), last_update_ts = ?, version = nextval('attr_kv_version_seq') " +
            "WHERE entity_id = ? and attr_key = ? RETURNING version;";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO t_attr (entity_id, attr_key, str_v, long_v, dbl_v, bool_v, json_v, last_update_ts, version) " +
                    "VALUES(?, ?, ?, ?, ?, ?, cast(? AS json), ?, nextval('attr_kv_version_seq')) " +
                    "ON CONFLICT (entity_id, attr_key) " +
                    "DO UPDATE SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v = cast(? AS json), last_update_ts = ?, version = nextval('attr_kv_version_seq') RETURNING version;";

    // 合并自 AbstractInsertRepository 的方法
    protected String replaceNullChars(String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }

    // 合并自 AbstractVersionedInsertRepository 的方法
    public List<Integer> saveOrUpdate(List<Attribute> entities) {
        return transactionTemplate.execute(status -> {
            List<Integer> seqNumbers = new ArrayList<>(entities.size());

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int[] updateResult = onBatchUpdate(entities, keyHolder);

            List<Map<String, Object>> seqNumbersList = keyHolder.getKeyList();

            int notUpdatedCount = entities.size() - seqNumbersList.size();

            List<Integer> toInsertIndexes = new ArrayList<>(notUpdatedCount);
            List<Attribute> insertEntities = new ArrayList<>(notUpdatedCount);
            for (int i = 0, keyHolderIndex = 0; i < updateResult.length; i++) {
                if (updateResult[i] == 0) {
                    insertEntities.add(entities.get(i));
                    seqNumbers.add(null);
                    toInsertIndexes.add(i);
                } else {
                    seqNumbers.add((Integer) seqNumbersList.get(keyHolderIndex).get("version"));
                    keyHolderIndex++;
                }
            }

            if (insertEntities.isEmpty()) {
                return seqNumbers;
            }

            int[] insertResult = onInsertOrUpdate(insertEntities, keyHolder);

            seqNumbersList = keyHolder.getKeyList();

            for (int i = 0, keyHolderIndex = 0; i < insertResult.length; i++) {
                if (insertResult[i] != 0) {
                    seqNumbers.set(toInsertIndexes.get(i), (Integer) seqNumbersList.get(keyHolderIndex).get("version"));
                    keyHolderIndex++;
                }
            }

            return seqNumbers;
        });
    }

    private int[] onBatchUpdate(List<Attribute> entities, KeyHolder keyHolder) {
        return jdbcTemplate.batchUpdate(new SequencePreparedStatementCreator(getBatchUpdateQuery()), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setOnBatchUpdateValues(ps, i, entities);
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        }, keyHolder);
    }

    private int[] onInsertOrUpdate(List<Attribute> insertEntities, KeyHolder keyHolder) {
        return jdbcTemplate.batchUpdate(new SequencePreparedStatementCreator(getInsertOrUpdateQuery()), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setOnInsertOrUpdateValues(ps, i, insertEntities);
            }

            @Override
            public int getBatchSize() {
                return insertEntities.size();
            }
        }, keyHolder);
    }

    protected void setOnBatchUpdateValues(PreparedStatement ps, int i, List<Attribute> entities) throws SQLException {
        Attribute kvEntity = entities.get(i);
        ps.setString(1, replaceNullChars(kvEntity.getStrV()));

        if (kvEntity.getLongV() != null) {
            ps.setLong(2, kvEntity.getLongV());
        } else {
            ps.setNull(2, Types.BIGINT);
        }

        if (kvEntity.getDblV() != null) {
            ps.setDouble(3, kvEntity.getDblV());
        } else {
            ps.setNull(3, Types.DOUBLE);
        }

        if (kvEntity.getBoolV() != null) {
            ps.setBoolean(4, kvEntity.getBoolV());
        } else {
            ps.setNull(4, Types.BOOLEAN);
        }

        ps.setString(5, replaceNullChars(kvEntity.getJsonV()));

        ps.setLong(6, kvEntity.getLastUpdateTs());
        ps.setObject(7, kvEntity.getEntityId());
        ps.setString(8, kvEntity.getAttrKey());
    }

    protected void setOnInsertOrUpdateValues(PreparedStatement ps, int i, List<Attribute> insertEntities) throws SQLException {
        Attribute kvEntity = insertEntities.get(i);
        ps.setObject(1, kvEntity.getEntityId());
        ps.setString(2, kvEntity.getAttrKey());

        ps.setString(3, replaceNullChars(kvEntity.getStrV()));
        ps.setString(9, replaceNullChars(kvEntity.getStrV()));

        if (kvEntity.getLongV() != null) {
            ps.setLong(4, kvEntity.getLongV());
            ps.setLong(10, kvEntity.getLongV());
        } else {
            ps.setNull(4, Types.BIGINT);
            ps.setNull(10, Types.BIGINT);
        }

        if (kvEntity.getDblV() != null) {
            ps.setDouble(5, kvEntity.getDblV());
            ps.setDouble(11, kvEntity.getDblV());
        } else {
            ps.setNull(5, Types.DOUBLE);
            ps.setNull(11, Types.DOUBLE);
        }

        if (kvEntity.getBoolV() != null) {
            ps.setBoolean(6, kvEntity.getBoolV());
            ps.setBoolean(12, kvEntity.getBoolV());
        } else {
            ps.setNull(6, Types.BOOLEAN);
            ps.setNull(12, Types.BOOLEAN);
        }

        ps.setString(7, replaceNullChars(kvEntity.getJsonV()));
        ps.setString(13, replaceNullChars(kvEntity.getJsonV()));

        ps.setLong(8, kvEntity.getLastUpdateTs());
        ps.setLong(14, kvEntity.getLastUpdateTs());
    }

    protected String getBatchUpdateQuery() {
        return BATCH_UPDATE;
    }

    protected String getInsertOrUpdateQuery() {
        return INSERT_OR_UPDATE;
    }

    private record SequencePreparedStatementCreator(String sql) implements PreparedStatementCreator, SqlProvider {

        private static final String[] COLUMNS = {"version"};

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement(sql, COLUMNS);
        }

        @Override
        public String getSql() {
            return this.sql;
        }
    }
}
