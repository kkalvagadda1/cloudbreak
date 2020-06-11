package com.sequenceiq.datalake.flow.dr.event;

import com.sequenceiq.datalake.entity.SdxDatabaseDrStatus;

public class DatalakeDatabaseRestoreStartEvent extends DatalakeDatabaseDrStartBaseEvent {
    private String databaseHost;

    private String backupLocation;

    public DatalakeDatabaseRestoreStartEvent(String selector, Long sdxId, String userId,
            String databaseHost, String backupLocation) {
        super(selector, sdxId, userId, SdxDatabaseDrStatus.SdxDatabaseDrStatusTypeEnum.RESTORE);
        this.databaseHost = databaseHost;
        this.backupLocation = backupLocation;
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    @Override
    public String selector() {
        return "DatalakeDatabaseRestoreStartEvent";
    }
}
