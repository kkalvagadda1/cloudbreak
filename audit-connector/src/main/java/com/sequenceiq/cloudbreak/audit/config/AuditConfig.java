package com.sequenceiq.cloudbreak.audit.config;


import com.sequenceiq.cloudbreak.audit.converter.AttemptAuditEventResultBuilderUpdater;
import com.sequenceiq.cloudbreak.audit.converter.AuditEventBuilderUpdater;
import com.sequenceiq.cloudbreak.util.NullUtil;
import io.netty.util.internal.StringUtil;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class AuditConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditConfig.class);

    @Value("${altus.audit.host:}")
    private String endpoint;

    @Value("${altus.audit.port:8989}")
    private int port;

    @Inject
    private List<AuditEventBuilderUpdater> auditEventBuilderUpdaterImplementations;

    @Inject
    private List<AttemptAuditEventResultBuilderUpdater> attemptAuditEventResultBuilderUpdaterImplementations;

    public String getEndpoint() {
        return endpoint;
    }

    public int getPort() {
        return port;
    }

    public boolean isConfigured() {
        return !StringUtil.isNullOrEmpty(endpoint);
    }

    @Bean
    public Map<Class, AuditEventBuilderUpdater> eventDataUtilizers() {
        Map<Class, AuditEventBuilderUpdater> result = new LinkedHashMap<>(auditEventBuilderUpdaterImplementations.size());
        auditEventBuilderUpdaterImplementations.forEach(utilizer -> result.put(utilizer.getType(), utilizer));
        if (MapUtils.isNotEmpty(result)) {
            List<String> eventDataUtilizers = result.entrySet().stream().map(classAuditEventDataUtilizerEntry -> {
                return String.format("[%s :: %s]",
                        NullUtil.getIfNotNull(classAuditEventDataUtilizerEntry.getKey(), t -> t.getSimpleName()),
                        NullUtil.getIfNotNull(classAuditEventDataUtilizerEntry.getValue(), u -> u.getClass().getSimpleName()));
            }).collect(Collectors.toList());
            String utilizerListMessage = String.join(",", eventDataUtilizers);
            LOGGER.debug("The " + AuditEventBuilderUpdater.class.getSimpleName() + " has the following implementations: {}", utilizerListMessage);
        } else {
            LOGGER.debug("The " + AuditEventBuilderUpdater.class.getSimpleName() + " has no any implementation!");
        }
        return result;
    }

    @Bean
    public Map<Class, AttemptAuditEventResultBuilderUpdater> auditEventDataUtilizers() {
        Map<Class, AttemptAuditEventResultBuilderUpdater> result = new LinkedHashMap<>(attemptAuditEventResultBuilderUpdaterImplementations.size());
        attemptAuditEventResultBuilderUpdaterImplementations.forEach(utilizer -> result.put(utilizer.getType(), utilizer));
        if (MapUtils.isNotEmpty(result)) {
            List<String> auditEventDataUtilizers = result.entrySet().stream().map(classAuditEventDataUtilizerEntry -> {
                return String.format("[%s :: %s]",
                        NullUtil.getIfNotNull(classAuditEventDataUtilizerEntry.getKey(), t -> t.getSimpleName()),
                        NullUtil.getIfNotNull(classAuditEventDataUtilizerEntry.getValue(), u -> u.getClass().getSimpleName()));
            }).collect(Collectors.toList());
            result.forEach((type, utilizer) -> auditEventDataUtilizers.add(String.format("[%s :: %s]",
                    NullUtil.getIfNotNull(type, t -> t.getSimpleName()),
                    NullUtil.getIfNotNull(utilizer, u -> u.getClass().getSimpleName()))));
            String utilizerListMessage = String.join(",", auditEventDataUtilizers);
            LOGGER.debug("The " + AttemptAuditEventResultBuilderUpdater.class.getSimpleName() + " has the following implementations: {}", utilizerListMessage);
        } else {
            LOGGER.debug("The " + AttemptAuditEventResultBuilderUpdater.class.getSimpleName() + " has no any implementation!");
        }
        return result;
    }

}
