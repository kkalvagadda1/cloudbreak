package com.sequenceiq.redbeams.flow.redbeams.provision.event.hostmetadatasetup;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class HostMetadataSetupRequest extends StackEvent {
    public HostMetadataSetupRequest(Long stackId) {
        super(stackId);
    }
}
