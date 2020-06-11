package com.sequenceiq.datalake.flow.dr.event;

import com.sequenceiq.datalake.entity.SdxDatabaseDrStatus;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseDrStartBaseEvent extends SdxEvent  {
    private SdxDatabaseDrStatus drStatus;

    public DatalakeDatabaseDrStartBaseEvent(String selector, Long sdxId, String userId,
            SdxDatabaseDrStatus.SdxDatabaseDrStatusTypeEnum operationType) {
        super(selector, sdxId, userId);
        drStatus = new SdxDatabaseDrStatus(operationType, sdxId);
    }

    public SdxDatabaseDrStatus getDrStatus() {
        return drStatus;
    }
}
