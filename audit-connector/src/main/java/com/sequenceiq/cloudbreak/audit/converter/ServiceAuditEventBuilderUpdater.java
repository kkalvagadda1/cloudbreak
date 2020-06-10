package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class ServiceAuditEventBuilderUpdater implements AuditEventBuilderUpdater<ServiceEventData> {

    @Override
    public void updateAuditEventBuilderWithEventData(AuditProto.AuditEvent.Builder auditEventBuilder, ServiceEventData source) {
        AuditProto.ServiceEventData.Builder serviceEventDataBuilder = AuditProto.ServiceEventData.newBuilder();
        doIfTrue(source.getEventDetails(), StringUtils::isNotEmpty, serviceEventDataBuilder::setEventDetails);
        doIfTrue(source.getVersion(), StringUtils::isNotEmpty, serviceEventDataBuilder::setDetailsVersion);
        auditEventBuilder.setServiceEventData(serviceEventDataBuilder.build());
    }

    @Override
    public Class<ServiceEventData> getType() {
        return ServiceEventData.class;
    }

}
