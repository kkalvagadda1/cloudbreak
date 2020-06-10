package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.AttemptAuditEventResult;
import com.sequenceiq.cloudbreak.audit.model.ResultEventData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter.class);

    private final Map<Class, AttemptAuditEventResultBuilderUpdater> auditEventDataUtilizers;

    public AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter(Map<Class, AttemptAuditEventResultBuilderUpdater> auditEventDataUtilizers) {
        this.auditEventDataUtilizers = auditEventDataUtilizers;
    }

    public AuditProto.AttemptAuditEventResult convert(AttemptAuditEventResult source) {
        AuditProto.AttemptAuditEventResult.Builder attemptAuditEventResultBuilder = prepareBuilderForCreateAuditEvent(source);
        updateResultEventData(attemptAuditEventResultBuilder, source.getResultEventData());
        return attemptAuditEventResultBuilder.build();
    }

    private AuditProto.AttemptAuditEventResult.Builder prepareBuilderForCreateAuditEvent(AttemptAuditEventResult source) {
        AuditProto.AttemptAuditEventResult.Builder builder = AuditProto.AttemptAuditEventResult.newBuilder()
                .setId(source.getId())
                .setResultCode(source.getResultCode());
        doIfTrue(source.getResultMessage(), StringUtils::isNotEmpty, builder::setResultMessage);
        return builder;
    }

    private void updateResultEventData(AuditProto.AttemptAuditEventResult.Builder auditEventBuilder, ResultEventData source) {
        if (source == null) {
            LOGGER.debug("No ResultEventData has provided to update AuditEventData hence no operation will be done.");
            return;
        }
        if (auditEventDataUtilizers.containsKey(source.getClass())) {
            auditEventDataUtilizers.get(source.getClass()).updateAttemptAuditEventResultBuilderWithEventData(auditEventBuilder, source);
        } else {
            throw new IllegalArgumentException("ResultEventData has an invalid class: " + source.getClass().getName());
        }
    }

}
