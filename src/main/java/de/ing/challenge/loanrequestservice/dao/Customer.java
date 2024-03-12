package de.ing.challenge.loanrequestservice.dao;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "CUSTOMER")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Customer {
    @Id
    @Column(name = "ID", nullable = false)
    private long id;

    @Column(name = "FULL_NAME", nullable = false)
    @Nonnull
    private String fullName;
}
