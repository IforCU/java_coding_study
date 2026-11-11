package kr.banklearning.operations.domain;

public record Profile(
        String username, String displayName, String email, String accountHint, String note) {}
