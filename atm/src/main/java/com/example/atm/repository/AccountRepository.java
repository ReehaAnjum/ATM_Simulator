package com.example.atm.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.atm.model.Account;

public interface AccountRepository extends MongoRepository<Account, String> {
    Account findByPin(String pin);
}
