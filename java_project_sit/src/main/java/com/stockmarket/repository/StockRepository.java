package com.stockmarket.repository;

import com.stockmarket.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    @Query("SELECT s FROM Stock s ORDER BY s.changePercent DESC")
    List<Stock> findTopGainers(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM Stock s ORDER BY s.changePercent ASC")
    List<Stock> findTopLosers(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM Stock s ORDER BY s.changePercent DESC")
    List<Stock> findAllOrderByChangePercentDesc();

    List<Stock> findBySector(String sector);

    @Query("SELECT s FROM Stock s WHERE s.symbol IN :symbols")
    List<Stock> findBySymbols(List<String> symbols);
}
