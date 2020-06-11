package com.sequenceiq.datalake.entity;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "sdxDatabaseDrstatus", uniqueConstraints = @UniqueConstraint(columnNames = {"operationId"}))
public class SdxDatabaseDrStatus {
    public enum SdxDatabaseDrStatusTypeEnum {
        BACKUP,
        RESTORE
    }

    public enum Status {
        INIT,
        TRIGGERRED,
        INPROGRESS,
        SUCCEEDED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sdx_status_generator")
    @SequenceGenerator(name = "sdx_status_generator", sequenceName = "sdxstatus_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SdxDatabaseDrStatusTypeEnum operationType;

    @NotNull
    private Long sdxClusterId;

    private String operationId;

    private String statusReason;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;

    public SdxDatabaseDrStatus() {
    }

    public SdxDatabaseDrStatus(SdxDatabaseDrStatusTypeEnum operationType, long sdxClusterId) {
        this.operationId = UUID.randomUUID().toString();
        this.operationType = operationType;
        this.sdxClusterId = sdxClusterId;
        this.status = SdxDatabaseDrStatus.Status.INIT;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Long getSdxClusterId() {
        return sdxClusterId;
    }

    public SdxDatabaseDrStatusTypeEnum getOperationType() {
        return operationType;
    }

    public void setOperationType(SdxDatabaseDrStatusTypeEnum operationType) {
        this.operationType = operationType;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Status getStatus() {
        return status;
    }

    public void setSdxClusterId(Long sdxClusterId) {
        this.sdxClusterId = sdxClusterId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

}
