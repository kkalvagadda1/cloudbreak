package com.sequenceiq.datalake.flow.dr;

import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.SdxDatabaseDrStatus;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseRestoreCouldNotStartEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseRestoreSuccessEvent;
import com.sequenceiq.datalake.flow.dr.event.DatalakeDatabaseRestoreWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.dr.SdxDrService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatalakeDatabaseRestoreActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDatabaseRestoreActions.class);

    private static final String OPERATION_ID = "OPERATION-ID";

    @Inject
    private SdxDrService sdxDrService;

    @Bean(name = "DATALAKE_DATABASE_RESTORE_START_STATE")
    public Action<?, ?> datalakeRestore() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseRestoreStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void prepareExecution(DatalakeDatabaseRestoreStartEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(OPERATION_ID, payload.getDrStatus().getOperationId());
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreStartEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Datalake database restore has been started for {}", payload.getResourceId());
                sdxDrService.databaseRestore(payload.getDrStatus(), payload.getResourceId(), payload.getDatabaseHost(), payload.getBackupLocation());
                sendEvent(context, DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseRestoreStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreCouldNotStartEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE")
    public Action<?, ?> datalakeRestoreInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Datalake database restore is in progress for {} ", payload.getResourceId());
                String operationId = (String) variables.get(OPERATION_ID);
                sendEvent(context, DatalakeDatabaseRestoreWaitRequest.from(context, operationId));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE")
    public Action<?, ?> restoreCouldNotStart() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreCouldNotStartEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseRestoreCouldNotStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreCouldNotStartEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database restore could not be started for datalake with id: {}", payload.getResourceId(), exception);
                String operationId = (String) variables.get(OPERATION_ID);
                sdxDrService.updateDatabaseStatusEntry(operationId, SdxDatabaseDrStatus.Status.FAILED, exception.getLocalizedMessage());
                sendEvent(context, DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseRestoreCouldNotStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_FINISHED_STATE")
    public Action<?, ?> finishedRestoreAction() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseRestoreSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Sdx database restore is finalized with sdx id: {}", payload.getResourceId());
                sdxDrService.updateDatabaseStatusEntry(payload.getOperationId(), SdxDatabaseDrStatus.Status.SUCCEEDED, null);
                sendEvent(context, DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseRestoreSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_DATABASE_RESTORE_FAILED_STATE")
    public Action<?, ?> restoreFailed() {
        return new AbstractSdxAction<>(DatalakeDatabaseRestoreFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeDatabaseRestoreFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeDatabaseRestoreFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                LOGGER.error("Datalake database restore could not be started for datalake with id: {}", payload.getResourceId(), exception);
                String operationId = (String) variables.get(OPERATION_ID);
                sdxDrService.updateDatabaseStatusEntry(operationId, SdxDatabaseDrStatus.Status.FAILED, exception.getLocalizedMessage());
                sendEvent(context, DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeDatabaseRestoreFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return DatalakeDatabaseRestoreFailedEvent.from(payload, ex);
            }
        };
    }
}
