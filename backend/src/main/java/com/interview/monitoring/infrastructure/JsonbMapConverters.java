package com.interview.monitoring.infrastructure;

import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * 設計說明：Spring Data JDBC 預設不知道如何把 Map<String, Object> 寫入 JSONB column；
 * 必須註冊 WritingConverter (Map → PGobject) 與 ReadingConverter (PGobject → Map)。
 * <p>
 * ObjectMapper 採用 new ObjectMapper() 而非注入，原因：
 * 1. @DataJdbcTest slice 不含 Jackson auto-configuration，注入會失敗。
 * 2. JSONB payload 的序列化需求單純（純 Map<String,Object>），不需要 Spring 管理的自訂配置。
 * 3. 避免 @DataJdbcTest 必須額外 import JacksonAutoConfiguration，降低測試上下文複雜度。
 * <p>
 * PGobject type 設為 "jsonb" 以符合 PostgreSQL JSONB column 型別。
 */
@Configuration
class JsonbMapConverters {

    @Bean
    JdbcCustomConversions jdbcCustomConversions() {
        var om = new ObjectMapper();
        return new JdbcCustomConversions(List.of(
                new MapToJsonbWriter(om),
                new JsonbToMapReader(om)
        ));
    }

    @WritingConverter
    static class MapToJsonbWriter implements Converter<Map<String, Object>, PGobject> {
        private final ObjectMapper om;

        MapToJsonbWriter(ObjectMapper om) { this.om = om; }

        @Override
        public PGobject convert(Map<String, Object> source) {
            try {
                var pg = new PGobject();
                pg.setType("jsonb");
                pg.setValue(om.writeValueAsString(source));
                return pg;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize Map to JSONB", e);
            }
        }
    }

    @ReadingConverter
    @SuppressWarnings("unchecked")
    static class JsonbToMapReader implements Converter<PGobject, Map<String, Object>> {
        private final ObjectMapper om;

        JsonbToMapReader(ObjectMapper om) { this.om = om; }

        @Override
        public Map<String, Object> convert(PGobject source) {
            try {
                return om.readValue(source.getValue(), Map.class);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize JSONB to Map", e);
            }
        }
    }
}
