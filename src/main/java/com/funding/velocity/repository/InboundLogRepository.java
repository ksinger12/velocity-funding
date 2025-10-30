package com.funding.velocity.repository;

import com.funding.velocity.entity.InboundLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboundLogRepository extends CrudRepository<InboundLog, Long> {

}
