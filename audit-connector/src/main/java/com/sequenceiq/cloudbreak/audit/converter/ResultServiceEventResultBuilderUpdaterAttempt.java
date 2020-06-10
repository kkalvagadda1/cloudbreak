package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ResultServiceEventData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class ResultServiceEventResultBuilderUpdaterAttempt implements AttemptAuditEventResultBuilderUpdater<ResultServiceEventData> {

    @Override
    public void updateAttemptAuditEventResultBuilderWithEventData(AuditProto.AttemptAuditEventResult.Builder builder, ResultServiceEventData source) {
        AuditProto.ResultServiceEventData.Builder resultServiceEventDataBuilder = AuditProto.ResultServiceEventData.newBuilder()
                .addAllResourceCrn(source.getResourceCrns());
        doIfTrue(source.getResultDetails(), StringUtils::isNotEmpty, resultServiceEventDataBuilder::setResultDetails);

        builder.setResultServiceEventData(resultServiceEventDataBuilder.build());
    }

    @Override
    public Class<ResultServiceEventData> getType() {
        return ResultServiceEventData.class;
    }

}
