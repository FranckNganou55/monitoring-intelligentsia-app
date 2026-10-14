package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * Maps the SSAtm Oracle table — one row per cassette slot per ATM.
 * Multiple rows share the same terminalId, differentiated by the cassette field (1, 2, 3, 4).
 */
@Entity
@Table(name = "SSAtm")
@Getter
public class SSAtm {

    @Id
    private Integer id;

    private Long pid;

    private String institution;

    private Long terminalId;

    private String adress;

    private String ville;

    /** ISO numeric currency code (e.g. 952 for XOF) */
    private Long currency;

    /** Number of notes remaining in this cassette */
    private Double nombreRest;

    private Double nombreRej;

    private Double nombreDep;

    /** Note denomination value */
    private Double note;

    /** Cassette slot number: 1.0, 2.0, 3.0, or 4.0 */
    private Double cassette;

    private Long statusId;

    private String statusDesc;
}
