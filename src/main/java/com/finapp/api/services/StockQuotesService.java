package com.finapp.api.services;

import com.finapp.api.entity.*;
import com.finapp.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StockQuotesService {

    private static Logger log = LoggerFactory.getLogger(StockQuotesService.class);

    private final QuoteRepository quoteRepository;
    private final StockRepository stockRepository;
    private final EarningRepository earningRepository;
    private final RatioRepository ratioRepository;
    private final ExtremumRepository extremumRepository;

    private final AlphaVantageApiService alphaVantageApiService;
    private final MacrotrendsApiService macrotrendsApiService;

    private final String PERIOD_ANNUAL = "ANNUAL";
    private final String PERIOD_QUARTERLY = "QUARTERLY";

    public String updateQuotes() {
        // update stock quotes for all symbols in db and update after last value in db (if empty then full update)

        // get date of last quote in db
        //StockQuote stockQuote = new StockQuote();
        log.info("Start update quotes");
        LocalDate dateLastQuote;
        for (Stock stock : stockRepository.findAll()) {
            //stockQuote.setStock(stock);
            dateLastQuote = quoteRepository.findFirstByStockOrderByDateDesc(stock).map(Quote::getDate)
                    .orElse(LocalDate.of(1970,1,1));
            if (ChronoUnit.DAYS.between(dateLastQuote, LocalDate.now()) > 1) {
                log.info("Start update symbol <" + stock.getSymbol() + "> from date " + dateLastQuote);
                // Get stock quotes missed in db (after <dateLastQuote>)
                List<Quote> listQuotes = alphaVantageApiService.getListStockQuoteFromDate(stock, dateLastQuote);
                quoteRepository.saveAll(listQuotes);
                log.info("Added " + listQuotes.size() + " new quotes");
                try {
                    Thread.sleep(15000); // Pause on 15 seconds
                } catch (Exception e) {
                }
            }
        }
        log.info("Update quotes finished");
        return "OK";
    }

    public String updateEps() {
        for (Stock stock : stockRepository.findAll()) {
            HashMap<Long, Float> annualEps = macrotrendsApiService.getAnnualEps(stock);
            List<Earning> listEps = earningRepository.findAllByCompanyAndPeriod(stock.getCompany(), PERIOD_ANNUAL);
            for (Earning entry: listEps) {
                if (annualEps.containsKey(entry.getYear())) {
                    entry.setEps(annualEps.get(entry.getYear()));
                    annualEps.remove(entry.getYear());
                }
            }
            for(Map.Entry<Long, Float> entry : annualEps.entrySet()) {
                listEps.add(new Earning(stock.getCompany(), PERIOD_ANNUAL, entry.getKey(), entry.getValue()));
            }
            earningRepository.saveAll(listEps);
            HashMap<LocalDate, Float> quarterlyEps = macrotrendsApiService.getQuarterlyEps(stock);
            listEps = earningRepository.findAllByCompanyAndPeriod(stock.getCompany(), PERIOD_QUARTERLY);
            for (Earning entry: listEps) {
                if (quarterlyEps.containsKey(entry.getDate())) {
                    entry.setEps(quarterlyEps.get(entry.getDate()));
                    quarterlyEps.remove(entry.getDate());
                }
            }
            for(Map.Entry<LocalDate, Float> entry : quarterlyEps.entrySet()) {
                listEps.add(new Earning(stock.getCompany(), PERIOD_QUARTERLY, entry.getKey(), entry.getValue()));
            }
            earningRepository.saveAll(listEps);
            log.info("Update " + listEps.size() + " new quarterly EPS for stock " + stock.getSymbol());
        }
        log.info("EPS update successfully finished.");
        return "OK";
    };

    public String updateRatios() {
        Ratio ratio;
        for (Stock stock : stockRepository.findAll()) {
            // Get ratios from Macrotrends Service
            ratio = stock.getRatio().orElse(new Ratio());
            ratio.setBps(macrotrendsApiService.getRatio(MacrotrendsApiService.RatioType.BPS, stock).orElse(ratio.getBps()));
            ratio.setCurrentRatio(macrotrendsApiService.getRatio(MacrotrendsApiService.RatioType.CUR_RATIO, stock).orElse(ratio.getCurrentRatio()));
            ratio.setDebtToEquity(macrotrendsApiService.getRatio(MacrotrendsApiService.RatioType.DEBT_EQUITY, stock).orElse(ratio.getDebtToEquity()));
            ratio.setRoe(macrotrendsApiService.getRatio(MacrotrendsApiService.RatioType.ROE, stock).orElse(ratio.getRoe()));
            ratio.setRoa(macrotrendsApiService.getRatio(MacrotrendsApiService.RatioType.ROA, stock).orElse(ratio.getRoa()));
            ratio.setRoi(macrotrendsApiService.getRatio(MacrotrendsApiService.RatioType.ROI, stock).orElse(ratio.getRoi()));
            // Calculate P/E & P/B from database
            float price = quoteRepository.findFirstByStockOrderByDateDesc(stock).map(Quote::getClose).orElse(0f);
            List<Earning> listEarning = earningRepository.findFirst3ByCompanyAndPeriodOrderByYearDesc(stock.getCompany(), PERIOD_ANNUAL);
            float sumEps = 0f;
            for (Earning entry : listEarning) {
                sumEps += entry.getEps();
            }
            ratio.setPe((sumEps != 0f) ? price * listEarning.size() / sumEps : 0f);
            listEarning = earningRepository.findFirst4ByCompanyAndPeriodOrderByDateDesc(stock.getCompany(), PERIOD_QUARTERLY);
            sumEps = 0f;
            for (Earning entry : listEarning) {
                sumEps += entry.getEps();
            }
            ratio.setTtmPe((sumEps != 0f) ? price / sumEps : 0f);
            ratio.setPb((ratio.getBps() != null && ratio.getBps() != 0) ? price / ratio.getBps() : 0f);
            // Save ratios to database
            ratioRepository.save(ratio);
            if (stock.getRatio().isEmpty()) {
                    stock.setRatio(ratio);
                    stockRepository.save(stock);
            }
            log.info("Ratios update for " + stock.getCompany().getName());
        }
        log.info("Ratios update successfully finished.");
        return "OK";
    };

    public String updateExtremums() {
        Extremum extremum;
        Float value;
        for (Stock stock : stockRepository.findAll()) {
            Float price = quoteRepository.findFirstByStockOrderByDateDesc(stock).map(Quote::getClose).orElse(0F);
            if (price != 0) {
                // Calculate extremums for stocks prices
                extremum  = stock.getExtremum().orElse(new Extremum());
                value = quoteRepository.findFirstByStockAndDateAfterOrderByHighDesc(stock, LocalDate.of(2018,1,1)).map(Quote::getHigh).orElse(0F);
                extremum.setDownFromMax(value != 0 ? (1 - price/value) * 100 : null);
                value = quoteRepository.findFirstByStockAndDateAfterAndDateBeforeOrderByLow(stock, LocalDate.of(2018,1,1), LocalDate.of(2020,3,1)).map(Quote::getLow).orElse(0F);
                extremum.setUpFromMin2018(value != 0 ? (1 - value/price) * 100 : null);
                value = quoteRepository.findFirstByStockAndDateAfterAndDateBeforeOrderByLow(stock, LocalDate.of(2015,1,1), LocalDate.of(2020,3,1)).map(Quote::getLow).orElse(0F);
                extremum.setUpFromMin2016(value != 0 ? (1 - value/price) * 100 : null);
                value = quoteRepository.findFirstByStockAndDateAfterAndDateBeforeOrderByLow(stock, LocalDate.of(2007,1,1), LocalDate.of(2020,3,1)).map(Quote::getLow).orElse(0F);
                extremum.setUpFromMin2008(value != 0 ? (1 - value/price) * 100 : null);
                value = quoteRepository.findFirstByStockAndDateAfterAndDateBeforeOrderByLow(stock, LocalDate.of(2000,1,1), LocalDate.of(2020,3,1)).map(Quote::getLow).orElse(0F);
                extremum.setUpFromMin2000(value != 0 ? (1 - value/price) * 100 : null);
                // Save extremums to database
                extremumRepository.save(extremum);
                if (stock.getExtremum().isEmpty()) {
                    stock.setExtremum(extremum);
                    stockRepository.save(stock);
                }
                log.info("Extremums calculate for " + stock.getCompany().getName());
            }
        }
        log.info("Extremums calculation successfully finished.");
        return "OK";
    };

    public String updatePE() {
        for (Stock stock : stockRepository.findAll()) {
            List<Earning> listEarning = earningRepository.findFirst3ByCompanyAndPeriodOrderByYearDesc(stock.getCompany(), PERIOD_ANNUAL);
            float sumEps = 0f;
            for (Earning entry: listEarning) {
                sumEps = sumEps + entry.getEps();
            }
            float price = quoteRepository.findFirstByStockOrderByDateDesc(stock).map(Quote::getClose).orElse(0f);
            Ratio ratio = stock.getRatio().orElse(new Ratio());
            ratio.setPe((sumEps != 0f) ? price * listEarning.size() / sumEps : 0f);
            ratioRepository.save(ratio);
            if (stock.getRatio().isEmpty()) {
                stock.setRatio(ratio);
                stockRepository.save(stock);
            }
            log.info("Stock " + stock.getSymbol() + " - update P/E to " + ratio.getPe());
        }
        log.info("P/E update successfully finished.");
        return "OK";
    };

    public String getPE(String stockSymbol) {
        String result = "EPS: ";
        Stock stock = stockRepository.findFirstBySymbol(stockSymbol).get();
        List<Earning> listEarning = earningRepository.findFirst3ByCompanyAndPeriodOrderByYearDesc(stock.getCompany(), PERIOD_ANNUAL);
        float sumEps = 0f;
        for (Earning entry: listEarning) {
            sumEps = sumEps + entry.getEps();
            result = result + entry.getYear() + " - " + entry.getEps() + " | ";
        }
        float price = quoteRepository.findFirstByStockOrderByDateDesc(stock).map(Quote::getClose).orElse(0f);
        result = result + " Price = " + price + " | ";
        float ratio = (sumEps != 0f) ? price * listEarning.size() / sumEps : 0f;
        result = result + " Ratio = " + ratio + " | ";
        return result;
    };

    public String exportBestPE() {
        log.info("Export best P/E successfully finished.");
        return "OK";
    };


/*    public List<StockQuote> getMinQuotes(String stockSymbol) {
        // search global minimum
        // search local minimums
        return null;
    }*/

}
