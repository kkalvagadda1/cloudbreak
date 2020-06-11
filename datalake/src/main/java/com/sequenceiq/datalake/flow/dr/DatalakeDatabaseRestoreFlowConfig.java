package com.sequenceiq.datalake.flow.dr;


import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_COULD_NOT_START_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrEvent.DATALAKE_DATABASE_RESTORE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrState.DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrState.DATALAKE_DATABASE_RESTORE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrState.DATALAKE_DATABASE_RESTORE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrState.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrState.DATALAKE_DATABASE_RESTORE_START_STATE;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.dr.DatalakeDatabaseDrState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatalakeDatabaseRestoreFlowConfig extends AbstractFlowConfiguration<DatalakeDatabaseDrState, DatalakeDatabaseDrEvent>
        implements RetryableFlowConfiguration<DatalakeDatabaseDrEvent> {

    private static final List<Transition<DatalakeDatabaseDrState, DatalakeDatabaseDrEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeDatabaseDrState, DatalakeDatabaseDrEvent>()
                    .defaultFailureEvent(DATALAKE_DATABASE_RESTORE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_START_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_EVENT).noFailureEvent()

                    .from(DATALAKE_DATABASE_RESTORE_START_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE)
                    .failureEvent(DATALAKE_DATABASE_RESTORE_COULD_NOT_START_EVENT)

                    .from(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_FINISHED_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_SUCCESS_EVENT)
                    .failureState(DATALAKE_DATABASE_RESTORE_FAILED_STATE)
                    .failureEvent(DATALAKE_DATABASE_RESTORE_FAILED_EVENT)

                    .from(DATALAKE_DATABASE_RESTORE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT).defaultFailureEvent()

                    .from(DATALAKE_DATABASE_RESTORE_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeDatabaseDrState, DatalakeDatabaseDrEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_DATABASE_RESTORE_FAILED_STATE, DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT);

    public DatalakeDatabaseRestoreFlowConfig() {
        super(DatalakeDatabaseDrState.class, DatalakeDatabaseDrEvent.class);
    }

    @Override
    public DatalakeDatabaseDrEvent[] getEvents() {
        return DatalakeDatabaseDrEvent.values();
    }

    @Override
    public DatalakeDatabaseDrEvent[] getInitEvents() {
        return new DatalakeDatabaseDrEvent[]{
                DATALAKE_DATABASE_RESTORE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "SDX Database Restore";
    }

    @Override
    protected List<Transition<DatalakeDatabaseDrState, DatalakeDatabaseDrEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DatalakeDatabaseDrState, DatalakeDatabaseDrEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeDatabaseDrEvent getRetryableEvent() {
        return DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT;
    }
}