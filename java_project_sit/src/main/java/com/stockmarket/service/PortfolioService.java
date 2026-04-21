package com.stockmarket.service;

import com.stockmarket.model.Portfolio;
import com.stockmarket.model.User;
import com.stockmarket.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    @Transactional(readOnly = true)
    public List<Portfolio> getUserPortfolio(User user) {
        return portfolioRepository.findByUserWithStock(user);
    }

    public BigDecimal getTotalPortfolioValue(User user) {
        List<Portfolio> items = getUserPortfolio(user);
        return items.stream()
            .map(Portfolio::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalInvested(User user) {
        Double total = portfolioRepository.getTotalInvestedByUser(user);
        return total != null ? BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP)
                             : BigDecimal.ZERO;
    }

    public BigDecimal getTotalProfitLoss(User user) {
        BigDecimal currentValue = getTotalPortfolioValue(user);
        BigDecimal totalInvested = getTotalInvested(user);
        return currentValue.subtract(totalInvested).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalProfitLossPercent(User user) {
        BigDecimal totalInvested = getTotalInvested(user);
        if (totalInvested.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getTotalProfitLoss(user)
               .divide(totalInvested, 4, RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100))
               .setScale(2, RoundingMode.HALF_UP);
    }
}
