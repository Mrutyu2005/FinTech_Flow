package com.fintech.accountservice.repository;

import com.fintech.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByOwnerUsername(String ownerUsername);
}
