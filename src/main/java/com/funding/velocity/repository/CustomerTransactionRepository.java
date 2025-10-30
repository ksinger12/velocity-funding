package com.funding.velocity.repository;

import com.funding.velocity.entity.CustomerTransaction;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerTransactionRepository extends CrudRepository<CustomerTransaction, Long> {


  @Query("""
          select sum(load_amount) from customer_transaction
          where time between :day and :endOfDay
      """)
  Double findSumOfLoadedAmountByDay(@Param("day") String day, @Param("endOfDay") String endOfDay);

}
