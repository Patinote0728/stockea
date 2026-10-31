package co.stockea.account.domain;

public enum PasswordPolicyViolation {
    TOO_SHORT,
    SAME_AS_CURRENT_PASSWORD,
    IN_PASSWORD_HISTORY,
    CONTAINS_DOCUMENT_NUMBER,
    CONTAINS_EMAIL
}