package com.sequenceiq.datalake.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.datalake.entity.SdxDatabaseDrStatus;

@Repository
@Transactional(Transactional.TxType.REQUIRED)
public interface SDxDatabaseDrStatusRepository extends CrudRepository<SdxDatabaseDrStatus, Long> {
    SdxDatabaseDrStatus findSdxDatabaseDrStatusByOperationId(String operationId);

    SdxDatabaseDrStatus findSdxDatabaseDrStatusBySdxClusterId(long sdxClusterId);
}
