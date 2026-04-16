package com.fintech.transactionservice.repository;

import com.fintech.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccountIdOrToAccountId(Long fromId, Long toId);

    List<Transaction> findByInitiatedByOrderByTimestampDesc(String username);

    @Query("SELECT t FROM Transaction t WHERE t.initiatedBy = :username " +
           "OR t.fromAccountId IN (:accountIds) " +
           "OR t.toAccountId IN (:accountIds) " +
           "ORDER BY t.timestamp DESC")
    List<Transaction> findByInitiatedByOrAccountIds(
            @Param("username") String username, 
            @Param("accountIds") List<Long> accountIds);

    List<Transaction> findAllByOrderByTimestampDesc();

    /**
     * Calculates total amount successfully transferred FROM a given account
     * since a given time — used for daily limit enforcement.
     *
     * NOTE: Pass Transaction.TransactionStatus.SUCCESS as the 'status' param.
     * Using a @Param avoids Hibernate 6 issues with inline enum class paths in JPQL.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.fromAccountId = :accountId " +
           "AND t.status = :status " +
           "AND t.timestamp >= :startOfDay")
    Double sumSuccessfulTransfersSince(
            @Param("accountId") Long accountId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("status") Transaction.TransactionStatus status);
}
