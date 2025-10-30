package com.funding.velocity.repository;

import com.funding.velocity.entity.OutboundLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboundLogRepository extends CrudRepository<OutboundLog, Long> {

}
