package org.intelligentsia.monitoring.domain.monitoring.atm.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class AtmSnapshotDto {
    private Long id;
    private Long atmId;
    private String terminalId;
    private Instant snapshotTime;
    private String currency1Alpha; private Integer note1; private Integer quantity1;
    private String currency2Alpha; private Integer note2; private Integer quantity2;
    private String currency3Alpha; private Integer note3; private Integer quantity3;
    private String currency4Alpha; private Integer note4; private Integer quantity4;
    private String statusId; private String statusDesc;
    private Boolean allOk; private String statusMessage;
    private boolean threshold1Exceeded; private boolean threshold2Exceeded;
}
