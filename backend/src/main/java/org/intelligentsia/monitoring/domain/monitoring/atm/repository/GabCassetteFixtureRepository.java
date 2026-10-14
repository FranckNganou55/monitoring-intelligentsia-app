package org.intelligentsia.monitoring.domain.monitoring.atm.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intelligentsia.monitoring.domain.monitoring.atm.dto.GabCassettesDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/** Only explicitly registered Excel test GABs use this local fixture source. */
@Repository
public class GabCassetteFixtureRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public GabCassetteFixtureRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc; this.mapper = mapper;
    }
    public Optional<GabCassettesDto> findByAtmId(Long atmId) {
        return jdbc.query("SELECT payload::text FROM atm_test_cassette_fixtures WHERE atm_id = ?", (rs, row) -> {
            try { return mapper.readValue(rs.getString(1), GabCassettesDto.class); }
            catch (JsonProcessingException ex) { throw new IllegalStateException("Jeu de cassettes de test invalide.", ex); }
        }, atmId).stream().findFirst();
    }
}
