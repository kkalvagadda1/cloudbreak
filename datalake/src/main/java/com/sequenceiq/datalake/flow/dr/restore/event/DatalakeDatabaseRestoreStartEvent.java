package com.sequenceiq.datalake.flow.dr.restore.event;

import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseDrStartBaseEvent;

public class DatalakeDatabaseRestoreStartEvent extends DatalakeDatabaseDrStartBaseEvent {
    private final String backupId;

    private final String backupLocation;

    private final String rangerAdminGroup;

    public DatalakeDatabaseRestoreStartEvent(String selector, Long sdxId, String userId,
            String backupId, String backupLocation, String rangerAdminGroup) {
        super(selector, sdxId, userId, SdxOperationType.RESTORE);
        this.backupId = backupId;
        this.backupLocation = backupLocation;
        this.rangerAdminGroup = rangerAdminGroup;
    }

    public String getBackupId() {
        return backupId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getRangerAdminGroup() {
        return  rangerAdminGroup;
    }
}
