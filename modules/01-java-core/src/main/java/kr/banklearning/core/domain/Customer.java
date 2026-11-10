package kr.banklearning.core.domain;

import java.util.Objects;
import java.util.UUID;
import kr.banklearning.core.exception.InvalidCustomerException;

public record Customer(UUID id, String name) {
    public Customer {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new InvalidCustomerException();
        }
        name = name.strip();
    }
}
