package com.funding.velocity.repository;

import com.funding.velocity.entity.InboundLog;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InboundLogRepository extends CrudRepository<InboundLog, Long> {

  @Query("""
        select * from inbound_log
        where JSON_VALID(payload) = 1
         and json_unquote(json_extract(payload, '$.id')) = :requestId
         and json_unquote(json_extract(payload, '$.customer_id')) = :customerId
        limit 1
    """)
  Optional<InboundLog> findByPayloadIdAndPayloadCustomerId(@Param("requestId") String requestId,
                                                           @Param("customerId") String customerId);
}
