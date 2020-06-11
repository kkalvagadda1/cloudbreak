package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class BackupV4Response {
    private boolean backupResult;

    private String reason;

    private FlowIdentifier flowIdentifier;

    public BackupV4Response(boolean backupResult, String reason, FlowIdentifier flowIdentifier) {
        this.backupResult = backupResult;
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    public boolean isBackupResult() {
        return backupResult;
    }

    public void setBackupResult(boolean backupResult) {
        this.backupResult = backupResult;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "BackupV4Response{" +
                "backupResult=" + backupResult +
                ", reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
