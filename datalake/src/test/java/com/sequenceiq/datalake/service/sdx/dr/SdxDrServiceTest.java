package com.sequenceiq.datalake.service.sdx.dr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SDxDatabaseDrStatusRepository;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class SdxDrServiceTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String BACKUPLOCATION = "location/of/backup";

    private static final String DBHOST = "loclhost";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String BACKUP_OPERATION_ID = "backup-operation-id";

    private static final String RESTORE_OPERATION_ID = "restore-operation-id";

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SDxDatabaseDrStatusRepository sdxDatabaseDrStatusRepository;

    @InjectMocks
    private SdxDrService sdxDrService;

    private SdxCluster sdxCluster;

    @Before
    public void setUp() {
        sdxCluster = getValidSdxCluster();
        ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
    }

    @Test
    public void triggerDatabaseBackupSuccess() {
        when(databaseServerV4Endpoint.getByCrn(anyString())).thenReturn(getValidDatabaseServerV4Response());
        when(sdxReactorFlowManager.triggerDatalakeDatabaseBackupFlow(anyLong(), anyString(), anyString())).thenReturn(BACKUP_OPERATION_ID);
        SdxDatabaseBackupResponse backupResponse = sdxDrService.triggerDatabaseBackup(sdxCluster, BACKUPLOCATION);
        Assert.assertEquals(BACKUP_OPERATION_ID, backupResponse.getOperationId());
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeDatabaseBackupFlow(1L, DBHOST, BACKUPLOCATION);
    }

    @Test
    public void triggerDatabaseRestoreSuccess() {
        when(databaseServerV4Endpoint.getByCrn(anyString())).thenReturn(getValidDatabaseServerV4Response());
        when(sdxReactorFlowManager.triggerDatalakeDatabaseRestoreFlow(anyLong(), anyString(), anyString())).thenReturn(RESTORE_OPERATION_ID);
        SdxDatabaseRestoreResponse restoreResponse = sdxDrService.triggerDatabaseRestore(sdxCluster, BACKUPLOCATION);
        Assert.assertEquals(RESTORE_OPERATION_ID, restoreResponse.getOperationId());
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeDatabaseRestoreFlow(1L, DBHOST, BACKUPLOCATION);
    }

    @Test
    public void triggerDatabaseBackupFailure() {
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> sdxDrService.triggerDatabaseBackup(sdxCluster, BACKUPLOCATION));
        assertEquals("Invalid backup request, Datalake with Crn: " + "crn:sdxcluster" + " not found", exception.getMessage());
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeDatabaseBackupFlow(1L, DBHOST, BACKUPLOCATION);
    }

    @Test
    public void triggerDatabaseRestoreFailure() {
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> sdxDrService.triggerDatabaseRestore(sdxCluster, BACKUPLOCATION));
        assertEquals("Invalid restore request, Datalake with Crn: " + "crn:sdxcluster" + " not found", exception.getMessage());
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeDatabaseRestoreFlow(1L, DBHOST, BACKUPLOCATION);
    }

    private SdxCluster getValidSdxCluster() {
        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setDatabaseCrn("crn:sdxcluster");
        sdxCluster.setId(1L);
        return sdxCluster;
    }

    private DatabaseServerV4Response getValidDatabaseServerV4Response() {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setHost(DBHOST);
        return response;
    }
}
