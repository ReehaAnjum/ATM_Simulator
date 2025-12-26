package com.example.atm.service;

import org.springframework.stereotype.Service;
import com.example.atm.model.Account;
import com.example.atm.repository.AccountRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
        // compute greedy denomination breakdown
        int remaining = (int) amount;
        Map<Integer, Integer> selection = new LinkedHashMap<>();
        for (int d : DENOMS) {
            int cnt = remaining / d;
            if (cnt > 0) selection.put(d, cnt);
            remaining -= cnt * d;
        }
        if (remaining != 0) return null;
        return withdraw(acc, amount, selection);
    }

    // Withdraw using user-provided denomination selection
    public Account withdraw(Account acc, double amount, Map<Integer, Integer> selection) {
        if (selection == null) return null;

        int total = 0;
        for (Map.Entry<Integer, Integer> e : selection.entrySet()) {
            int denom = e.getKey();
            int cnt = e.getValue() == null ? 0 : e.getValue();
            boolean valid = false;
            for (int d : DENOMS) if (d == denom) { valid = true; break; }
            if (!valid || cnt < 0) return null;
            total += denom * cnt;
        }

        if (total != (int) amount) return null;
        if (amount > acc.getBalance()) return null;

        acc.setBalance(acc.getBalance() - amount);

        StringBuilder sb = new StringBuilder();
        sb.append("Withdraw ₹").append((int) amount).append(" [");
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
    private boolean isValidDenomination(double amount) {
        int remaining = (int) amount;
        for (int d : DENOMS) {
            int cnt = remaining / d;
            remaining -= cnt * d;
        }
        return remaining == 0;
    }
}
