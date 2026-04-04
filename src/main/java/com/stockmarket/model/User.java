package com.stockmarket.model;

/*
 * Import JPA annotations for ORM mapping
 */
import jakarta.persistence.*;

/*
 * Import validation annotations
 */
import jakarta.validation.constraints.*;

/*
 * Lombok annotations to reduce boilerplate code
 */
import lombok.*;

/*
 * Java utility imports
 */
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * USER ENTITY CLASS
 * ============================================================
 * This class represents a user in the stock market system.
 * It contains:
 * - Authentication details
 * - Financial balance
 * - Role-based authorization
 * - Relationships with trades & portfolio
 * - Audit tracking
 * - Business logic methods
 * ============================================================
 */

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // ============================================================
    // PRIMARY KEY
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // AUTHENTICATION FIELDS
    // ============================================================

    /**
     * Username must be unique and not empty
     */
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Email validation with format check
     */
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Password should be secure (hashed in service layer)
     */
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Column(nullable = false)
    private String password;

    // ============================================================
    // PERSONAL INFORMATION
    // ============================================================

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    // ============================================================
    // ACCOUNT STATUS & SECURITY
    // ============================================================

    /**
     * Role for authorization (Spring Security compatible)
     */
    @Column(nullable = false)
    @Builder.Default
    private String role = "ROLE_USER";

    /**
     * Account enabled or disabled
     */
    @Builder.Default
    private boolean isEnabled = true;

    /**
     * Account locked status
     */
    @Builder.Default
    private boolean isAccountNonLocked = true;

    /**
     * Credential expiration flag
     */
    @Builder.Default
    private boolean isCredentialsNonExpired = true;

    /**
     * Account expiration flag
     */
    @Builder.Default
    private boolean isAccountNonExpired = true;

    // ============================================================
    // FINANCIAL DATA
    // ============================================================

    /**
     * User wallet balance
     * Default = $100,000 for simulation
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = new BigDecimal("100000.00");

    /**
     * Total profit/loss tracking
     */
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalProfitLoss = BigDecimal.ZERO;

    // ============================================================
    // AUDIT FIELDS
    // ============================================================

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // ============================================================
    // RELATIONSHIPS
    // ============================================================

    /**
     * One user can have many trades
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Trade> trades = new ArrayList<>();

    /**
     * One user can have many portfolio items
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Portfolio> portfolioItems = new ArrayList<>();

    // ============================================================
    // LIFECYCLE HOOKS
    // ============================================================

    /**
     * Automatically set update timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Before insert
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ============================================================
    // BUSINESS LOGIC METHODS
    // ============================================================

    /**
     * Add money to user balance
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }

    /**
     * Withdraw money from balance
     */
    public boolean withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 &&
                this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            return true;
        }
        return false;
    }

    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    /**
     * Add trade to user
     */
    public void addTrade(Trade trade) {
        trades.add(trade);
        trade.setUser(this);
    }

    /**
     * Add portfolio item
     */
    public void addPortfolioItem(Portfolio portfolio) {
        portfolioItems.add(portfolio);
        portfolio.setUser(this);
    }

    /**
     * Calculate total portfolio value
     */
    public BigDecimal calculatePortfolioValue() {
        BigDecimal total = BigDecimal.ZERO;

        for (Portfolio p : portfolioItems) {
            total = total.add(p.getTotalValue());
        }

        return total;
    }

    /**
     * Update profit/loss
     */
    public void updateProfitLoss(BigDecimal profit) {
        this.totalProfitLoss = this.totalProfitLoss.add(profit);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(this.role);
    }

    /**
     * Check if user has sufficient balance
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    /**
     * Reset password (example logic)
     */
    public void resetPassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * Disable account
     */
    public void disableAccount() {
        this.isEnabled = false;
    }

    /**
     * Enable account
     */
    public void enableAccount() {
        this.isEnabled = true;
    }

    // ============================================================
    // TO STRING SAFE (Avoid password exposure)
    // ============================================================

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", balance=" + balance +
                ", role='" + role + '\'' +
                '}';
    }
}
