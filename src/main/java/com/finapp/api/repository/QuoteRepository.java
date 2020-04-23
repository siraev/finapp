package com.finapp.api.repository;

import com.finapp.api.entity.Stock;
import com.finapp.api.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    Optional<Quote> findFirstByStockOrderByDateDesc(Stock stock);

    Optional<Quote> findFirstByStockAndDateAfterOrderByHighDesc(Stock stock, LocalDate date);

    Optional<Quote> findFirstByStockAndDateAfterAndDateBeforeOrderByLow(Stock stock, LocalDate dateAfter, LocalDate dateBefore);

}
