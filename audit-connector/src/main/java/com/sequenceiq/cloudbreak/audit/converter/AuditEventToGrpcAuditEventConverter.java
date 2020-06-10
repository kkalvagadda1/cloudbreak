package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ActorBase;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;
import static com.sequenceiq.cloudbreak.util.UuidUtil.uuidSupplier;

@Component
public class AuditEventToGrpcAuditEventConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEventToGrpcAuditEventConverter.class);

    private final Map<Class, AuditEventBuilderUpdater> eventDataUtilizer;

    public AuditEventToGrpcAuditEventConverter(Map<Class, AuditEventBuilderUpdater> eventDataUtilizer) {
        this.eventDataUtilizer = eventDataUtilizer;
    }

    public AuditProto.AuditEvent convert(AuditEvent source) {
        String id = Optional.ofNullable(source.getId()).orElseGet(uuidSupplier());
        String requestId = Optional.ofNullable(source.getRequestId()).orElseGet(uuidSupplier());
        AuditProto.AuditEvent.Builder auditEventBuilder = prepareBuilderForCreateAuditEvent(source, id, requestId);
        updateAuditEventActor(auditEventBuilder, source.getActor());
        updateAuditEventData(auditEventBuilder, source.getEventData());
        return auditEventBuilder.build();
    }

    private AuditProto.AuditEvent.Builder prepareBuilderForCreateAuditEvent(AuditEvent source, String id, String requestId) {
        AuditProto.AuditEvent.Builder builder = AuditProto.AuditEvent.newBuilder()
                .setId(id)
                .setTimestamp(System.currentTimeMillis())
                .setAccountId(source.getAccountId())
                .setRequestId(requestId)
                .setEventName(source.getEventName().name())
                .setEventSource(source.getEventSource().getName());
        doIfTrue(source.getSourceIp(), StringUtils::isNotEmpty, builder::setSourceIPAddress);
        return builder;
    }

    private void updateAuditEventActor(AuditProto.AuditEvent.Builder auditEventBuilder, ActorBase actorBase) {
        if (actorBase instanceof ActorCrn) {
            ActorCrn actor = (ActorCrn) actorBase;
            auditEventBuilder.setActorCrn(actor.getActorCrn());
        } else if (actorBase instanceof ActorService) {
            ActorService actor = (ActorService) actorBase;
            auditEventBuilder.setActorServiceName(actor.getActorServiceName());
        } else {
            throw new IllegalArgumentException("Actor has an invalid class: " + actorBase.getClass().getName());
        }
    }

    private void updateAuditEventData(AuditProto.AuditEvent.Builder auditEventBuilder, EventData source) {
        if (source == null) {
            LOGGER.debug("No EventData has provided to update AuditEventData hence no operation will be done.");
            return;
        }
        if (eventDataUtilizer.containsKey(source.getClass())) {
            eventDataUtilizer.get(source.getClass()).updateAuditEventBuilderWithEventData(auditEventBuilder, source);
        } else {
            throw new IllegalArgumentException("EventData has an invalid class: " + source.getClass().getName());
        }
    }

}
