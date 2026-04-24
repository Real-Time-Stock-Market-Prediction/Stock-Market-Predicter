package com.stockmarket.repository;

// Import Stock entity class
import com.stockmarket.model.Stock;

// Import JpaRepository for CRUD operations
import org.springframework.data.jpa.repository.JpaRepository;

// Import Query annotation for custom JPQL queries
import org.springframework.data.jpa.repository.Query;

// Marks this interface as Repository layer
import org.springframework.stereotype.Repository;

// Import List and Optional
import java.util.List;
import java.util.Optional;

// Repository annotation for Spring Data JPA
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // Find stock using stock symbol
    Optional<Stock> findBySymbol(String symbol);

    // Check if stock symbol already exists
    boolean existsBySymbol(String symbol);

    // Get top gaining stocks ordered by highest percentage change
    @Query("SELECT s FROM Stock s ORDER BY s.changePercent DESC")
    List<Stock> findTopGainers(org.springframework.data.domain.Pageable pageable);

    // Get top losing stocks ordered by lowest percentage change
    @Query("SELECT s FROM Stock s ORDER BY s.changePercent ASC")
    List<Stock> findTopLosers(org.springframework.data.domain.Pageable pageable);

    // Get all stocks ordered by highest change percentage
    @Query("SELECT s FROM Stock s ORDER BY s.changePercent DESC")
    List<Stock> findAllOrderByChangePercentDesc();

    // Find stocks by sector
    List<Stock> findBySector(String sector);

    // Find multiple stocks using list of symbols
    @Query("SELECT s FROM Stock s WHERE s.symbol IN :symbols")
    List<Stock> findBySymbols(List<String> symbols);
}
