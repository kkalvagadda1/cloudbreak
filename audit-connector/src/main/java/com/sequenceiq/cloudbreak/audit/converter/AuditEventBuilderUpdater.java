package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.EventData;

public interface AuditEventBuilderUpdater<T extends EventData> {

    void updateAuditEventBuilderWithEventData(AuditProto.AuditEvent.Builder auditEventBuilder, T source);

    Class<T> getType();

}
