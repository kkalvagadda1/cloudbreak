package com.sequenceiq.datalake.flow.dr.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseBackupSuccessEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseBackupWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxDrService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class DatalakeDatabaseBackupWaitHandler extends ExceptionCatcherEventHandler<DatalakeDatabaseBackupWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDatabaseBackupWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 90;

    @Inject
    private SdxDrService sdxDrService;

    @Override
    public String selector() {
        return "DatalakeDatabaseBackupWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new DatalakeDatabaseBackupFailedEvent(resourceId, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        DatalakeDatabaseBackupWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling datalake database backup  for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            sdxDrService.waitCloudbreakFlow(sdxId, pollingConfig, "Database backup");
            response = new DatalakeDatabaseBackupSuccessEvent(sdxId, userId, request.getOperationId());
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Database backup polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeDatabaseBackupFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Database backup poller stopped for cluster: {}", sdxId);
            response = new DatalakeDatabaseBackupFailedEvent(sdxId, userId,
                    new PollerStoppedException("Database backup timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Database backup polling failed for cluster: {}", sdxId);
            response = new DatalakeDatabaseBackupFailedEvent(sdxId, userId, exception);
        }
        sendEvent(response, event);
    }
}
