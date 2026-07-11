package com.prateek.ProjectExpenseManagement.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.function.Function;

public final class SqlParameterBatchBuilder {

    private SqlParameterBatchBuilder() {
    }

    public static <T> void batchUpdate(NamedParameterJdbcTemplate jdbcTemplate,
                                       String sql,
                                       List<T> items,
                                       Function<T, MapSqlParameterSource> mapper) {
        MapSqlParameterSource[] batch = items.stream()
                .map(mapper)
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
    }
}
