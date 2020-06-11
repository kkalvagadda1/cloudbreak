package com.sequenceiq.sdx.api.model;

public class SdxDatabaseBackupStatusResponse {
    private String status;

    private String statusReason;

    public SdxDatabaseBackupStatusResponse(String status) {
        this.status = status;
    }

    public SdxDatabaseBackupStatusResponse(String status, String statusReason) {
        this.status = status;
        this.statusReason = statusReason;

    }

    public String getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }
}
