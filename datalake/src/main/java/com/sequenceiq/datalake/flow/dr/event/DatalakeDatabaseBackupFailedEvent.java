package com.sequenceiq.datalake.flow.dr.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupFailedEvent extends SdxEvent {

    private final Exception exception;

    public DatalakeDatabaseBackupFailedEvent(Long sdxId, String userId, Exception exception) {
        super("DATALAKE_DATABASE_BACKUP_FAILED_EVENT", sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeDatabaseBackupFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeDatabaseBackupFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "DATALAKE_DATABASE_BACKUP_FAILED_EVENT";
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DATALAKE_DATABASE_BACKUP_FAILED_EVENT{");
        sb.append("exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
