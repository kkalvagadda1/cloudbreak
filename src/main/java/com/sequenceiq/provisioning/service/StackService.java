package com.sequenceiq.provisioning.service;

import java.util.Set;

import com.sequenceiq.provisioning.controller.json.IdJson;
import com.sequenceiq.provisioning.controller.json.StackJson;
import com.sequenceiq.provisioning.domain.User;

public interface StackService {

    StackJson get(User user, Long id);

    Set<StackJson> getAll(User user);

    IdJson create(User user, StackJson stackRequest);

    void delete(User user, Long id);

    Boolean startAll(User user, Long stackId);

    Boolean stopAll(User user, Long stackId);
}
