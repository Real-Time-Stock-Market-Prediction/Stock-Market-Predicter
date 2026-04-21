package com.stockmarket.controller;

import com.stockmarket.model.*;
import com.stockmarket.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final StockService stockService;
    private final PortfolioService portfolioService;
    private final TradeService tradeService;
    private final UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Stock> allStocks = stockService.getAllStocks();
        List<Stock> gainers = stockService.getTopGainers(5);
        List<Stock> losers = stockService.getTopLosers(5);

        model.addAttribute("user", user);
        model.addAttribute("stocks", allStocks);
        model.addAttribute("gainers", gainers);
        model.addAttribute("losers", losers);
        model.addAttribute("portfolioValue", portfolioService.getTotalPortfolioValue(user));
        model.addAttribute("totalProfitLoss", portfolioService.getTotalProfitLoss(user));
        model.addAttribute("totalProfitLossPercent", portfolioService.getTotalProfitLossPercent(user));
        return "dashboard";
    }

    @GetMapping("/portfolio")
    public String portfolio(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Portfolio> holdings = portfolioService.getUserPortfolio(user);

        model.addAttribute("user", user);
        model.addAttribute("holdings", holdings);
        model.addAttribute("portfolioValue", portfolioService.getTotalPortfolioValue(user));
        model.addAttribute("totalInvested", portfolioService.getTotalInvested(user));
        model.addAttribute("totalProfitLoss", portfolioService.getTotalProfitLoss(user));
        model.addAttribute("totalProfitLossPercent", portfolioService.getTotalProfitLossPercent(user));
        return "portfolio";
    }

    @GetMapping("/trades")
    public String trades(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Trade> trades = tradeService.getUserTrades(user);
        model.addAttribute("user", user);
        model.addAttribute("trades", trades);
        return "trades";
    }

    @GetMapping("/market")
    public String market(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Stock> stocks = stockService.getAllStocks();
        model.addAttribute("user", user);
        model.addAttribute("stocks", stocks);
        return "market";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String password,
            Model model) {
        try {
            userService.registerUser(username, email, fullName, password);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
