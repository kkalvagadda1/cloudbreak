package com.sequenceiq.datalake.flow.dr.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupCouldNotStartEvent extends SdxEvent {
    private final Exception exception;

    public DatalakeDatabaseBackupCouldNotStartEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeDatabaseBackupCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new DatalakeDatabaseBackupCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "DatalakeDatabaseBackupCouldNotStartEvent";
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DatalakeDatabaseBackupCouldNotStartEvent{");
        sb.append("exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
