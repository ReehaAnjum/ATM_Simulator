package com.example.atm.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "accounts")
public class Account {

    @Id
    private String id;
    private String username;
    private String pin;
    private double balance;

    private List<String> transactions = new ArrayList<>();

    public Account() {}

    public String getUsername() { return username; }
    public String getPin() { return pin; }
    public double getBalance() { return balance; }
    public List<String> getTransactions() { return transactions; }

    public void setBalance(double balance) { this.balance = balance; }
}
