package com.insurance.demo.repository;

import com.insurance.demo.model.db.ContractCreatedEvent;
import org.apache.tomcat.jni.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public interface ContractCreatedEventRepository extends JpaRepository<ContractCreatedEvent, Integer> {

     ArrayList<ContractCreatedEvent> findAllByDateBetween(LocalDate startDate, LocalDate endDate);

     @Query("SELECT e.premium FROM #{#entityName} e WHERE e.contractId = :contractId")
     List<Integer> getPremiumByContractId(@Param("contractId") Integer contractId);

     @Query(nativeQuery = true, value = "SELECT MONTH(e.start_date) FROM #{#entityName} e WHERE e.contract_Id = :contractId")
     Integer getMonthFromContractId(@Param("contractId") Integer contractId);



}