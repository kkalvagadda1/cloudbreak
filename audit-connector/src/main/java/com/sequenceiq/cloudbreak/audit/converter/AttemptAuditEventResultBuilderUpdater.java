package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ResultEventData;

public interface AttemptAuditEventResultBuilderUpdater<T extends ResultEventData> {

    void updateAttemptAuditEventResultBuilderWithEventData(AuditProto.AttemptAuditEventResult.Builder builder, T source);

    Class<T> getType();

}
