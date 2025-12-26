package com.example.atm.model;

import java.util.Map;

public class DenominationRequest {
    private String pin;
    private double amount;
    private Map<Integer, Integer> denominations;

    public DenominationRequest() {}

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Map<Integer, Integer> getDenominations() { return denominations; }
    public void setDenominations(Map<Integer, Integer> denominations) { this.denominations = denominations; }
}
