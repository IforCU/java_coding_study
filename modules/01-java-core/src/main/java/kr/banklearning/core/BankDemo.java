package kr.banklearning.core;

import java.math.BigDecimal;
import kr.banklearning.core.dto.AccountView;
import kr.banklearning.core.dto.CustomerView;

public final class BankDemo {
    private BankDemo() {}

    public static void main(String[] args) {
        BankService bank = new BankService();
        CustomerView customer = bank.createCustomer("학습고객01");
        AccountView first = bank.openAccount(customer.id());
        AccountView second = bank.openAccount(customer.id());
        bank.deposit(first.id(), new BigDecimal("100000"));
        bank.withdraw(first.id(), new BigDecimal("35000"));
        bank.deposit(second.id(), new BigDecimal("5000"));
        System.out.println("학습 고객: " + customer.name());
        System.out.println(
                "첫 번째 계좌 잔액: " + bank.getAccount(first.id()).balance().toPlainString() + "원");
        System.out.println(
                "두 번째 계좌 잔액: " + bank.getAccount(second.id()).balance().toPlainString() + "원");
        System.out.println("첫 번째 계좌 거래 내역:");
        bank.history(first.id())
                .forEach(
                        transaction ->
                                System.out.println(
                                        transaction.type()
                                                + " "
                                                + transaction.amount().toPlainString()
                                                + "원 / 잔액 "
                                                + transaction.balanceAfter().toPlainString()
                                                + "원"));
    }
}
