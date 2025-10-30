package com.funding.velocity.repository;

import com.funding.velocity.entity.CustomerTransaction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerTransactionRepository extends CrudRepository<CustomerTransaction, Long> {

}
