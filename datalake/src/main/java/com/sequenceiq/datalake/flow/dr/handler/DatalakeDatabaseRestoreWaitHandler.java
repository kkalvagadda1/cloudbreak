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
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseRestoreSuccessEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseRestoreWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxDrService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class DatalakeDatabaseRestoreWaitHandler extends ExceptionCatcherEventHandler<DatalakeDatabaseRestoreWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDatabaseRestoreWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 90;

    @Inject
    private SdxDrService sdxDrService;

    @Override
    public String selector() {
        return "DatalakeDatabaseRestoreWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new DatalakeDatabaseRestoreFailedEvent(resourceId, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        DatalakeDatabaseRestoreWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling datalake database restore for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            sdxDrService.waitCloudbreakFlow(sdxId, pollingConfig, "Database restore");
            response = new DatalakeDatabaseRestoreSuccessEvent(sdxId, userId, request.getOperationId());
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Database restore polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeDatabaseRestoreFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Database restore poller stopped for cluster: {}", sdxId);
            response = new DatalakeDatabaseRestoreFailedEvent(sdxId, userId,
                    new PollerStoppedException("Database restore timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Database restore polling failed for cluster: {}", sdxId);
            response = new DatalakeDatabaseRestoreFailedEvent(sdxId, userId, exception);
        }
        sendEvent(response, event);
    }
}
