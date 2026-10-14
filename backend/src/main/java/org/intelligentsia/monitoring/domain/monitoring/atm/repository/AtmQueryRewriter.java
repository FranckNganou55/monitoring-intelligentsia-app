package org.intelligentsia.monitoring.domain.monitoring.atm.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;
import java.util.Map;

/** Retain existing Java sort property names for the native status-filter query. */
public class AtmQueryRewriter implements QueryRewriter {
    @Override public String rewrite(String query, Sort sort) {
        for (var column : Map.of("terminalId", "terminal_id", "createdAt", "created_at",
                "updatedAt", "updated_at", "svfePid", "svfe_pid", "institutionId", "institution_id").entrySet())
            query = query.replace("a." + column.getKey(), "a." + column.getValue());
        return query;
    }
}
