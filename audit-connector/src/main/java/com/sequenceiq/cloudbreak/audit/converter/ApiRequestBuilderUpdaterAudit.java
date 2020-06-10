package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class ApiRequestBuilderUpdaterAudit implements AuditEventBuilderUpdater<ApiRequestData> {

    @Override
    public void updateAuditEventBuilderWithEventData(AuditProto.AuditEvent.Builder auditEventBuilder, ApiRequestData source) {
        AuditProto.ApiRequestData.Builder apiRequestDataBuilder = AuditProto.ApiRequestData.newBuilder()
                .setMutating(source.isMutating());
        doIfTrue(source.getApiVersion(), StringUtils::isNotEmpty, apiRequestDataBuilder::setApiVersion);
        doIfTrue(source.getRequestParameters(), StringUtils::isNotEmpty, apiRequestDataBuilder::setRequestParameters);
        doIfTrue(source.getUserAgent(), StringUtils::isNotEmpty, apiRequestDataBuilder::setUserAgent);
        auditEventBuilder.setApiRequestData(apiRequestDataBuilder.build());
    }

    @Override
    public Class<ApiRequestData> getType() {
        return ApiRequestData.class;
    }

}
