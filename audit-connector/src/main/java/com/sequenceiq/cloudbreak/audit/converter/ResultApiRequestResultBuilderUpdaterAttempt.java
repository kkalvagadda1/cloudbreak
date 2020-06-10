package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ResultApiRequestData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class ResultApiRequestResultBuilderUpdaterAttempt implements AttemptAuditEventResultBuilderUpdater<ResultApiRequestData> {

    @Override
    public void updateAttemptAuditEventResultBuilderWithEventData(AuditProto.AttemptAuditEventResult.Builder builder, ResultApiRequestData source) {
        AuditProto.ResultApiRequestData.Builder resultApiRequestDataBuilder = AuditProto.ResultApiRequestData.newBuilder();
        doIfTrue(source.getResponseParameters(), StringUtils::isNotEmpty, resultApiRequestDataBuilder::setResponseParameters);

        builder.setResultApiRequestData(resultApiRequestDataBuilder.build());
    }

    @Override
    public Class<ResultApiRequestData> getType() {
        return ResultApiRequestData.class;
    }

}
