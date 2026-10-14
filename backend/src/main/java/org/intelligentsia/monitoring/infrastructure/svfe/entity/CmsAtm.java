package org.intelligentsia.monitoring.infrastructure.svfe.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * In-memory aggregation of SSAtm rows (4 cassettes) for a single ATM.
 * Not a JPA entity — built by grouping SSAtm rows by terminalId.
 */
@Getter
@Setter
public class CmsAtm {

    private Long pid;
    private String institution;
    private String terminalId;
    private String adress;
    private String ville;
    private Long statusId;
    private String statusDesc;

    private Long currency1;
    private Double note1;
    private Double nbre1;

    private Long currency2;
    private Double note2;
    private Double nbre2;

    private Long currency3;
    private Double note3;
    private Double nbre3;

    private Long currency4;
    private Double note4;
    private Double nbre4;
}
