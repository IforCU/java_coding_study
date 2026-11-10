package kr.banklearning.operations.domain;

public record ProfileView(String username, String displayName, String email, String accountHint, String note) {
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getAccountHint() { return accountHint; }
    public String getNote() { return note; }
}
