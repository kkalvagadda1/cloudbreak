package com.sequenceiq.datalake.service.sdx;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowLogResponse;

@Service
public class CloudbreakFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowService.class);

    @Inject
    private FlowEndpoint flowEndpoint;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    public void getAndSaveLastCloudbreakFlowChainId(SdxCluster sdxCluster) {
        FlowLogResponse lastFlowByResourceName = flowEndpoint.getLastFlowByResourceName(sdxCluster.getClusterName());
        LOGGER.info("Found last flow from Cloudbreak, flowId: {} created: {} nextEvent:{} resourceId: {} stateStatus: {}",
                lastFlowByResourceName.getFlowId(),
                lastFlowByResourceName.getCreated(),
                lastFlowByResourceName.getNextEvent(),
                lastFlowByResourceName.getResourceId(),
                lastFlowByResourceName.getStateStatus());
        sdxCluster.setLastCbFlowChainId(lastFlowByResourceName.getFlowChainId());
        sdxClusterRepository.save(sdxCluster);
    }

    public FlowState getLastKnownFlowState(SdxCluster sdxCluster) {
        try {
            String actualCbFlowChainId = sdxCluster.getLastCbFlowChainId();
            if (actualCbFlowChainId != null) {
                LOGGER.info("Check if flow is running: {}", actualCbFlowChainId);
                Boolean hasActiveFlow = flowEndpoint.hasFlowRunningByChainId(sdxCluster.getLastCbFlowChainId()).getHasActiveFlow();
                if (hasActiveFlow) {
                    return FlowState.RUNNING;
                } else {
                    return FlowState.FINISHED;
                }
            } else  {
                return FlowState.UNKNOWN;
            }
        } catch (NotFoundException e) {
            LOGGER.error("Flow chain id or resource {} not found in CB: {}, so there is no active flow!", sdxCluster.getClusterName(), e.getMessage());
            return FlowState.UNKNOWN;
        } catch (Exception e) {
            LOGGER.error("Exception occured during checking if there is a flow for cluster {} in CB: {}", sdxCluster.getClusterName(), e.getMessage());
            return FlowState.UNKNOWN;
        }
    }

    public enum FlowState {
        RUNNING, FINISHED, UNKNOWN
    }
}
