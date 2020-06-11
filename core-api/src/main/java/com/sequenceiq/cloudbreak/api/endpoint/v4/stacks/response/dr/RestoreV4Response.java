package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class RestoreV4Response {
    private boolean restoreResult;

    private String reason;

    private FlowIdentifier flowIdentifier;

    public RestoreV4Response(boolean backupResult, String reason, FlowIdentifier flowIdentifier) {
        this.restoreResult = backupResult;
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    public boolean isRestoreResult() {
        return restoreResult;
    }

    public void setRestoreResult(boolean restoreResult) {
        this.restoreResult = restoreResult;
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
        return "RestoreV4Response{" +
                "backupResult=" + restoreResult +
                ", reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
