package com.sequenceiq.datalake.service.sdx.dr;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.FINISHED;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.RUNNING;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.BackupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.RestoreV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabaseDrStatus;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SDxDatabaseDrStatusRepository;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreStatusResponse;

/**
 * Service to perform backup/restore of the database backing SDX.
 */
@Component
public class SdxDrService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDrService.class);

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SDxDatabaseDrStatusRepository sdxDatabaseDrStatusRepository;

    public SdxDatabaseBackupResponse triggerDatabaseBackup(SdxCluster sdxCluster, String backupLocation) {
        // Identifying the database host talking to database service.
        String databaseCrn = sdxCluster.getDatabaseCrn();
        DatabaseServerV4Response databaseServerResponse =  databaseServerV4Endpoint.getByCrn(databaseCrn);
        if (databaseServerResponse == null || Strings.isNullOrEmpty(databaseServerResponse.getHost())) {
            throw new BadRequestException(String.format("Invalid backup request, Datalake with Crn: %s not found", databaseCrn));
        }
        String databaseHost = databaseServerResponse.getHost();

        String operationId = triggerDatalakeDatabaseBackupFlow(sdxCluster.getId(), databaseHost, backupLocation);
        return new SdxDatabaseBackupResponse(operationId);
    }

    public SdxDatabaseRestoreResponse triggerDatabaseRestore(SdxCluster sdxCluster, String backupLocation) {
        // Identifying the database host talking to database service.
        String databaseCrn = sdxCluster.getDatabaseCrn();
        DatabaseServerV4Response databaseServerResponse =  databaseServerV4Endpoint.getByCrn(databaseCrn);
        if (databaseServerResponse == null || Strings.isNullOrEmpty(databaseServerResponse.getHost())) {
            throw new BadRequestException(String.format("Invalid restore request, Datalake with Crn: %s not found", databaseCrn));
        }
        String databaseHost = databaseServerResponse.getHost();

        String operationId = triggerDatalakeDatabaseRestoreFlow(sdxCluster.getId(), databaseHost, backupLocation);
        return new SdxDatabaseRestoreResponse(operationId);

    }

    private String triggerDatalakeDatabaseBackupFlow(Long clusterId, String databaseHost, String backupLocation) {
        MDCBuilder.buildMdcContext(databaseHost);
        return sdxReactorFlowManager.triggerDatalakeDatabaseBackupFlow(clusterId, databaseHost, backupLocation);
    }

    private String triggerDatalakeDatabaseRestoreFlow(Long clusterId, String databaseHost, String backupLocation) {
        MDCBuilder.buildMdcContext(databaseHost);
        return sdxReactorFlowManager.triggerDatalakeDatabaseRestoreFlow(clusterId, databaseHost, backupLocation);
    }

    public void databaseBackup(SdxDatabaseDrStatus drStatus, Long clusterId, String databaseHost, String backupLocation) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(clusterId).orElseThrow(() -> new NotFoundException("Datalake with id: " + clusterId));
        try {
            sdxDatabaseDrStatusRepository.save(drStatus);
            BackupV4Response backupV4Response = stackV4Endpoint.backupDatabase(0L, databaseHost, backupLocation);
            // TODO remove below stub code after the initial review is complete.
            // BackupV4Response backupV4Response = new BackupV4Response(true, "dummy reason", new FlowIdentifier(FlowType.FLOW, "pollableId"));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, backupV4Response.getFlowIdentifier());
            updateDatabaseStatusEntry(drStatus.getOperationId(), SdxDatabaseDrStatus.Status.TRIGGERRED, null);
        } catch (WebApplicationException e) {
            String message = String.format("Database backup failed for datalake-id: [%d]. Message: [%s]", clusterId, e.getMessage());
            throw new CloudbreakApiException(message);
        }
    }

    public void databaseRestore(SdxDatabaseDrStatus drStatus, Long clusterId, String databaseHost, String backupLocation) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(clusterId).orElseThrow(() -> new NotFoundException("Datalake with id: " + clusterId));
        try {
            sdxDatabaseDrStatusRepository.save(drStatus);
            RestoreV4Response restoreV4Response = stackV4Endpoint.restoreDatabase(0L, databaseHost, backupLocation);
            // TODO remove below stub code after the initial review is complete.
//             RestoreV4Response restoreV4Response = new RestoreV4Response(true, "dummy reason", new FlowIdentifier(FlowType.FLOW, "pollableId"));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, restoreV4Response.getFlowIdentifier());
            updateDatabaseStatusEntry(drStatus.getOperationId(), SdxDatabaseDrStatus.Status.TRIGGERRED, null);
        } catch (WebApplicationException e) {
            String message = String.format("Database restore failed for datalake-id: [%d]. Message: [%s]", clusterId, e.getMessage());
            throw new CloudbreakApiException(message);
        }
    }

    public void waitCloudbreakFlow(Long id, PollingConfig pollingConfig, String pollingMessage) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> checkDatabaseDrStatus(sdxCluster, pollingMessage));
    }

    private AttemptResult<StackV4Response> checkDatabaseDrStatus(SdxCluster sdxCluster, String pollingMessage) throws JsonProcessingException {
        LOGGER.info("{} polling cloudbreak for stack status: '{}' in '{}' env", pollingMessage, sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("{} polling cancelled in inmemory store, id: {}", pollingMessage, sdxCluster.getId());
                return AttemptResults.breakFor(pollingMessage + " polling cancelled in inmemory store, id: " + sdxCluster.getId());
            } else {
                CloudbreakFlowService.FlowState flowState = cloudbreakFlowService.getLastKnownFlowState(sdxCluster);
                if (RUNNING.equals(flowState)) {
                    LOGGER.info("{} polling will continue, cluster has an active flow in Cloudbreak, id: {}", pollingMessage, sdxCluster.getId());
                    return AttemptResults.justContinue();
                } else {
                    return getStackResponseAttemptResult(sdxCluster, pollingMessage, flowState);
                }
            }
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    private AttemptResult<StackV4Response> getStackResponseAttemptResult(SdxCluster sdxCluster, String pollingMessage, CloudbreakFlowService.FlowState flowState)
            throws JsonProcessingException {
        StackV4Response stackV4Response = stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet());
//         TODO remove below stub code after the initial review is complete.
//        StackV4Response stackV4Response = new StackV4Response();
//        stackV4Response.setStatus(Status.AVAILABLE);
//        ClusterV4Response tempCluster = new ClusterV4Response();
//        tempCluster.setStatus(Status.AVAILABLE);
//        tempCluster.setName("testdl");
//        stackV4Response.setCluster(tempCluster);
//        stackV4Response.setName("testdl");
        LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
        ClusterV4Response cluster = stackV4Response.getCluster();
        if (stackAndClusterAvailable(stackV4Response, cluster)) {
            return sdxDrSucceeded(stackV4Response);
        } else {
            if (Status.BACKUP_FAILED.equals(stackV4Response.getStatus()) ||
                    Status.RESTORE_FAILED.equals(stackV4Response.getStatus())) {
                LOGGER.info("{} failed for Stack {} with status {}", pollingMessage, stackV4Response.getName(), stackV4Response.getStatus());
                return sdxDrFailed(sdxCluster, stackV4Response.getStatusReason(), pollingMessage);
            } else if (Status.BACKUP_FAILED.equals(stackV4Response.getCluster().getStatus()) ||
                    Status.RESTORE_FAILED.equals(stackV4Response.getCluster().getStatus())) {
                LOGGER.info("{} failed for Cluster {} status {}", pollingMessage, stackV4Response.getCluster().getName(),
                        stackV4Response.getCluster().getStatus());
                return sdxDrFailed(sdxCluster, stackV4Response.getCluster().getStatusReason(), pollingMessage);
            } else {
                if (FINISHED.equals(flowState)) {
                    LOGGER.info("Flow finished, but Backup/Restore is not complete: {}", sdxCluster.getClusterName());
                    return sdxDrFailed(sdxCluster, "stack is in improper state", pollingMessage);
                } else {
                    return AttemptResults.justContinue();
                }
            }
        }
    }

    private AttemptResult<StackV4Response> sdxDrFailed(SdxCluster sdxCluster, String statusReason, String pollingMessage) {
        LOGGER.info("{} failed, statusReason: {}", pollingMessage, statusReason);
        return AttemptResults.breakFor("SDX " + pollingMessage + " failed '" + sdxCluster.getClusterName() + "', " + statusReason);
    }

    private AttemptResult<StackV4Response> sdxDrSucceeded(StackV4Response stackV4Response) {
        LOGGER.info("Database DR operation doe SDX cluster {} is successfull", stackV4Response.getCluster().getName());
        return AttemptResults.finishWith(stackV4Response);
    }

    private boolean stackAndClusterAvailable(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isAvailable()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isAvailable();
    }

    /**
     * Updates the status of the database backup/restore operation.
     * @param operationId Operation Id
     * @param status Status of the operation
     * @param failedReason Failure reason, if any.
     */
    public void updateDatabaseStatusEntry(String operationId, SdxDatabaseDrStatus.Status status, String failedReason) {
        if (Strings.isNullOrEmpty(operationId)) {
            return;
        }
        SdxDatabaseDrStatus drStatus = sdxDatabaseDrStatusRepository.findSdxDatabaseDrStatusByOperationId(operationId);
        drStatus.setStatus(status);
        if (!Strings.isNullOrEmpty(failedReason)) {
            drStatus.setStatusReason(failedReason);
        }
        sdxDatabaseDrStatusRepository.save(drStatus);
    }

    /**
     * Gets the status of the database backup operation.
     * @param sdxCluster Sdx cluster on which the backup operation is performed.
     * @param operationId Operation Id
     * @return Backup status
     */
    public SdxDatabaseBackupStatusResponse getDatabaseBackupStatus(SdxCluster sdxCluster, String operationId) {
        SdxDatabaseDrStatus drStatus = sdxDatabaseDrStatusRepository.findSdxDatabaseDrStatusByOperationId(operationId);
        if ((drStatus == null) || (!drStatus.getSdxClusterId().equals(sdxCluster.getId()))
                || (!drStatus.getOperationType().equals(SdxDatabaseDrStatus.SdxDatabaseDrStatusTypeEnum.BACKUP))) {
            String message = String.format("Invalid operation-id: [%s]. provided", operationId);
            throw new CloudbreakApiException(message);
        }
        return new SdxDatabaseBackupStatusResponse(drStatus.getStatus().name(), drStatus.getStatusReason());
    }

    /**
     * Gets the status of the database restore operation.
     * @param sdxCluster Sdx cluster on which the restore operation is performed.
     * @param operationId Operation Id
     * @return Restore status
     */
    public SdxDatabaseRestoreStatusResponse getDatabaseRestoreStatus(SdxCluster sdxCluster, String operationId) {
        SdxDatabaseDrStatus drStatus = sdxDatabaseDrStatusRepository.findSdxDatabaseDrStatusByOperationId(operationId);
        if ((drStatus == null) || (!drStatus.getSdxClusterId().equals(sdxCluster.getId()))
        || (!drStatus.getOperationType().equals(SdxDatabaseDrStatus.SdxDatabaseDrStatusTypeEnum.RESTORE))) {
            String message = String.format("Invalid operation-id: [%s]. provided", operationId);
            throw new CloudbreakApiException(message);
        }
        return new SdxDatabaseRestoreStatusResponse(drStatus.getStatus().name(), drStatus.getStatusReason());
    }
}
