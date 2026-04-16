package com.fintech.accountservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor

public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String ownerUsername;

    @Column(nullable = false)
    private Double balance;

    @Column(nullable = false)
    private String accountType;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 10)
    private String mobile;

    @Column(nullable = false, unique = true, length = 12)
    private String aadhaar;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String transactionPassword;

}
