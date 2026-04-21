package com.stockmarket.config;

import com.stockmarket.service.StockService;
import com.stockmarket.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final StockService stockService;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing application data...");

        // Seed demo user
        try {
            userService.registerUser("demo", "demo@stockmarket.com", "Demo Trader", "demo123");
            log.info("Demo user created: username=demo, password=demo123");
        } catch (IllegalArgumentException e) {
            log.info("Demo user already exists.");
        }

        // Seed stocks
        stockService.initializeStocks();

        log.info("Data initialization complete.");
    }
}
