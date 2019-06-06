package com.sequenceiq.redbeams.flow.chain;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.stack.StackEvent;
import com.sequenceiq.redbeams.flow.stack.provision.StackProvisionEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

@Component
public class ProvisionFlowEventChainFactory implements FlowEventChainFactory<StackEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.PROVISION_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackEvent event) {

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(StackProvisionEvent.START_CREATION_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new StackEvent(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.event(), event.getResourceId()));
        return flowEventChain;
    }
}
