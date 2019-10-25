package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_HOST_JOIN_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_INSTALL_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STARTED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.INSTALL_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.START_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.START_OPERATION_STATE;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintUpdater;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariAdapter.ClusterStatusResult;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.json.AnonymizerUtil;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterSetupService implements ClusterSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterSetupService.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientFactory clientFactory;

    @Inject
    private AmbariClusterConnectorPollingResultChecker ambariClusterConnectorPollingResultChecker;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private AmbariOperationService ambariOperationService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private AmbariViewProvider ambariViewProvider;

    @Inject
    private AmbariClusterTemplateSubmitter ambariClusterTemplateSubmitter;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private HostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Inject
    private AmbariPollingServiceProvider ambariPollingServiceProvider;

    @Inject
    private CentralBlueprintUpdater centralBlueprintUpdater;

    @Inject
    private AmbariClusterCreationSuccessHandler ambariClusterCreationSuccessHandler;

    @Inject
    private AmbariSmartSenseCapturer ambariSmartSenseCapturer;

    @Inject
    private AmbariAdapter ambariAdapter;

    @Override
    public void waitForServer(Stack stack) throws CloudbreakException {
        AmbariClient defaultClient = clientFactory.getDefaultAmbariClient(stack);
        AmbariClient client = clientFactory.getAmbariClient(stack, stack.getCluster());
        PollingResult pollingResult = ambariPollingServiceProvider.ambariStartupPollerObjectPollingService(stack, defaultClient, client);
        if (isSuccess(pollingResult)) {
            LOGGER.info("Ambari has successfully started! Polling result: {}", pollingResult);
        } else if (isExited(pollingResult)) {
            throw new CancellationException("Polling of Ambari server start has been cancelled.");
        } else {
            LOGGER.info("Could not start Ambari. polling result: {}", pollingResult);
            throw new CloudbreakException(String.format("Could not start Ambari. polling result: '%s'", pollingResult));
        }
    }

    @Override
    public void buildCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            clusterService.updateCreationDateOnCluster(cluster);
            AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
            Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
            TemplatePreparationObject templatePreparationObject = conversionService.convert(stack, TemplatePreparationObject.class);
            Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(hostGroups);
            Set<InstanceMetaData> runningInstanceMetaDataSet = stack.getRunningInstanceMetaDataSet();

            recipeEngine.executePostAmbariStartRecipes(stack, hostGroupService.getRecipesByCluster(cluster.getId()));
            ambariRepositoryVersionService.setBaseRepoURL(stack.getName(), cluster.getId(), stack.getOrchestrator(), ambariClient);
            String blueprintText = centralBlueprintUpdater.getBlueprintText(templatePreparationObject);
            addBlueprint(stack.getId(), ambariClient, blueprintText, cluster.getTopologyValidation());
            cluster.setExtendedBlueprintText(blueprintText);
            clusterService.updateCluster(cluster);
            PollingResult waitForHostsResult = ambariPollingServiceProvider.hostsPollingService(stack, ambariClient, runningInstanceMetaDataSet);
            ambariClusterConnectorPollingResultChecker
                    .checkPollingResult(waitForHostsResult, cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_HOST_JOIN_FAILED.code()));
            ambariClusterTemplateSubmitter.addClusterTemplate(cluster, hostGroupMappings, ambariClient);
            Pair<PollingResult, Exception> pollingResult =
                    ambariOperationService.waitForOperationsToStart(stack, ambariClient, singletonMap("INSTALL_START", 1), START_OPERATION_STATE);

            String message =
                    pollingResult.getRight() == null ? constructClusterFailedMessage(cluster.getId(), ambariClient) : pollingResult.getRight().getMessage();
            ambariClusterConnectorPollingResultChecker.checkPollingResult(pollingResult.getLeft(), message);
            Pair<PollingResult, Exception> pollingResultExceptionPair = ambariOperationService
                    .waitForOperations(stack, ambariClient, Map.of("CLUSTER_INSTALL", 1), INSTALL_AMBARI_PROGRESS_STATE);

            ambariClusterConnectorPollingResultChecker
                    .checkPollingResult(pollingResultExceptionPair.getLeft(), constructClusterFailedMessage(cluster.getId(), ambariClient));
            recipeEngine.executePostInstallRecipes(stack);
            ambariSmartSenseCapturer.capture(0, ambariClient);
            cluster = ambariViewProvider.provideViewInformation(ambariClient, cluster);
            ambariClusterCreationSuccessHandler.handleClusterCreationSuccess(runningInstanceMetaDataSet, cluster);
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            LOGGER.info("Error while building the Ambari cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    private String constructClusterFailedMessage(Long clusterId, AmbariClient ambariClient) {
        String ambariClusterInstallFailedMsg = cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_INSTALL_FAILED.code());
        ClusterStatusResult clusterStatusResult = ambariAdapter.getClusterStatusHostComponentMap(ambariClient);
        LOGGER.info("There are not started services. Cluster: [{}], services: [{}]", clusterId, clusterStatusResult.getComponentsInStatus());
        return String.format("%s Not started services: [%s]", ambariClusterInstallFailedMsg, clusterStatusResult.getComponentsInStatus());
    }

    @Override
    public void waitForHosts(Stack stack) {
        ambariPollingServiceProvider
                .hostsPollingService(
                        stack,
                        clientFactory.getAmbariClient(stack, stack.getCluster()),
                        stack.getRunningInstanceMetaDataSet());
    }

    @Override
    public void waitForServices(Stack stack, int requestId) throws CloudbreakException {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        LOGGER.info("Waiting for Hadoop services to start on stack");
        PollingResult servicesStartResult = ambariOperationService
                .waitForOperations(stack, ambariClient, singletonMap("start services", requestId), START_AMBARI_PROGRESS_STATE).getLeft();
        if (isExited(servicesStartResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
        } else if (isTimeout(servicesStartResult)) {
            throw new CloudbreakException("Timeout while starting Ambari services.");
        }
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STARTED.code()));
    }

    @Override
    public boolean available(Stack stack) {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        return ambariPollingServiceProvider.isAmbariAvailable(stack, ambariClient);
    }

    private void addBlueprint(Long stackId, AmbariClient ambariClient, String blueprintText, Boolean topologyValidation) {
        try {
            LOGGER.info("Adding generated blueprint to Ambari: {}", AnonymizerUtil.anonymize(JsonUtil.minify(blueprintText)));
            ambariClient.addBlueprint(blueprintText, topologyValidation);
        } catch (HttpResponseException hre) {
            if (hre.getStatusCode() == HttpStatus.SC_CONFLICT) {
                LOGGER.info("Ambari blueprint already exists for stack: {}", stackId);
            } else {
                throw new CloudbreakServiceException("Ambari blueprint could not be added: " + AmbariClientExceptionUtil.getErrorMessage(hre), hre);
            }
        } catch (URISyntaxException | IOException e) {
            throw new CloudbreakServiceException("Ambari blueprint could not be added: " + e.getMessage(), e);
        }
    }

}