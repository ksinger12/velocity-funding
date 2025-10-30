package com.funding.velocity.repository;

import com.funding.velocity.entity.CustomerTransaction;
import java.time.ZonedDateTime;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerTransactionRepository extends CrudRepository<CustomerTransaction, Long> {


  @Query("""
          select sum(load_amount) from customer_transaction
          where time between :startDatetime and :endDatetime
          and customer_id = :customerId
      """)
  Double findSumOfLoadedAmountBetweenDatesByCustomerId(@Param("startDatetime") ZonedDateTime startDatetime,
                                                       @Param("endDatetime") ZonedDateTime endDatetime,
                                                       @Param("customerId") String customerId);

  @Query("""
          select count(*) from customer_transaction
          where time between :startDatetime and :endDatetime
          and customer_id = :customerId
      """)
  int findNumberOfTransactionsByCustomerIdAndTime(@Param("startDatetime") ZonedDateTime startDatetime,
                                                  @Param("endDatetime") ZonedDateTime endDatetime,
                                                  @Param("customerId") String customerId);

}
