package com.sequenceiq.datalake.flow.dr.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupSuccessEvent extends SdxEvent {
    private String operationId;

    public DatalakeDatabaseBackupSuccessEvent(Long sdxId, String userId, String operationId) {
        super("DATALAKE_DATABASE_BACKUP_SUCCESS_EVENT", sdxId, userId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String selector() {
        return "DATALAKE_DATABASE_BACKUP_SUCCESS_EVENT";
    }
}
