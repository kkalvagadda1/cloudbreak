package com.sequenceiq.datalake.flow.dr.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseRestoreCouldNotStartEvent extends SdxEvent {
    private final Exception exception;

    public DatalakeDatabaseRestoreCouldNotStartEvent(Long sdxId, String userId, Exception exception) {
        super("DatalakeDatabaseRestoreCouldNotStartEvent", sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeDatabaseRestoreCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new DatalakeDatabaseRestoreCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "DatalakeDatabaseRestoreCouldNotStartEvent";
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DatalakeDatabaseRestoreCouldNotStartEvent{");
        sb.append("exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
