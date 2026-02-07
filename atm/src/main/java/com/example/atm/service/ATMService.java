package com.example.atm.service;

import org.springframework.stereotype.Service;
import com.example.atm.model.Account;
import com.example.atm.repository.AccountRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ATMService {

    private final AccountRepository repo;

    private static final int[] DENOMS = {500, 200, 100, 50, 20, 10};

    public ATMService(AccountRepository repo) {
        this.repo = repo;
    }

    // LOGIN
    public Account login(String pin) {
        return repo.findByPin(pin);
    }

    // BALANCE
    public double getBalance(Account acc) {
        return acc.getBalance();
    }

    // DEPOSIT
    public Account deposit(Account acc, double amount) {
        acc.setBalance(acc.getBalance() + amount);
        addTransaction(acc, "Deposit ₹" + amount);
        return repo.save(acc);
    }

    // WITHDRAW WITH DENOMINATION CHECK
    // Withdraw using greedy fallback (computes denominations automatically)
    public Account withdraw(Account acc, double amount) {
        if (acc == null) return null;
        if (amount <= 0) return null;
        if (amount != (int) amount) return null; // only integer rupee amounts supported
        int amt = (int) amount;
        if (amt > (int) acc.getBalance()) return null;

        // compute greedy denomination breakdown
        int remaining = amt;
        Map<Integer, Integer> selection = new HashMap<>();
        for (int d : DENOMS) {
            int cnt = remaining / d;
            if (cnt > 0) selection.put(d, cnt);
            remaining -= cnt * d;
        }
        if (remaining != 0) return null;
        return withdraw(acc, amt, selection);
    }

    // Withdraw using user-provided denomination selection
    public Account withdraw(Account acc, double amount, Map<Integer, Integer> selection) {
        if (acc == null || selection == null) return null;
        if (amount <= 0) return null;
        if (amount != (int) amount) return null;

        int total = 0;
        for (Map.Entry<Integer, Integer> e : selection.entrySet()) {
            Integer denom = e.getKey();
            Integer cnt = e.getValue();
            if (denom == null || cnt == null) return null;
            if (cnt < 0) return null;
            boolean valid = false;
            for (int d : DENOMS) if (d == denom) { valid = true; break; }
            if (!valid) return null;
            total += denom * cnt;
        }

        int amt = (int) amount;
        if (total != amt) return null;
        if (amt > (int) acc.getBalance()) return null;

        acc.setBalance(acc.getBalance() - amt);

        StringBuilder sb = new StringBuilder();
        sb.append("Withdraw ₹").append(amt).append(" [");
        boolean first = true;
        for (int d : DENOMS) {
            int c = selection.getOrDefault(d, 0);
            if (c > 0) {
                if (!first) sb.append(", ");
                sb.append(c).append("x").append(d);
                first = false;
            }
        }
        sb.append("]");

        addTransaction(acc, sb.toString());
        return repo.save(acc);
    }

    // MINI STATEMENT
    public void addTransaction(Account acc, String msg) {
        acc.getTransactions().add(
            LocalDateTime.now() + " | " + msg + " | Balance ₹" + acc.getBalance()
        );
    }

    // DENOMINATION LOGIC
    // legacy helper removed — denomination validation handled in withdraw methods
}
