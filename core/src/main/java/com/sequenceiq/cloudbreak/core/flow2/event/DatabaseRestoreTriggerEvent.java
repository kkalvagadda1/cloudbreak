package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

import reactor.rx.Promise;

public class DatabaseRestoreTriggerEvent extends BackupRestoreEvent {

    public DatabaseRestoreTriggerEvent(String selector, Long stackId, String backupLocation, String backupId, String rangerAdminGroup) {
        super(selector, stackId, backupLocation, backupId, rangerAdminGroup);
    }

    public DatabaseRestoreTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted,
            String backupLocation, String backupId, String rangerAdminGroup) {
        super(event, resourceId, accepted, backupLocation, backupId, rangerAdminGroup);
    }
}
