package com.finapp.api.services;

import com.finapp.api.entity.Company;
import com.finapp.api.entity.Ratio;
import com.finapp.api.entity.Stock;
import com.finapp.api.repository.CompanyRepository;
import com.finapp.api.repository.RatioRepository;
import com.finapp.api.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InitDataService {

    private static Logger log = LoggerFactory.getLogger(StockQuotesService.class);

    private final CompanyRepository companyRepository;
    private final StockRepository stockRepository;
    private final RatioRepository ratioRepository;

    private final MacrotrendsApiService macrotrendsApiService;

    public String loadStocksForSPBExchange() {
        String csvFile = "data/ListingSecurityList.csv";
        String cvsSplitBy = ";";
        String result = "";
        String line = "";
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] data = line.split(cvsSplitBy);
                addStock(data[1], data[2]);
                count++;
            }
        } catch (IOException e) {
            log.info(e.getLocalizedMessage(), e);
        }
        log.info("Load stocks for SPB Exchange: " + count + " stocks added.");
        return count + " stocks added.";
    };

    private void addStock(String stockSymbol, String companyName) {
        Company company = companyRepository.findFirstByName(companyName).orElse(new Company());
        if (company.getId() == null) {
            company.setName(companyName);
            companyRepository.save(company);
            log.info("Company " + companyName + " added.");
        }
        Stock stock = stockRepository.findFirstBySymbol(stockSymbol).orElse(new Stock());
        if (stock.getId() == null) {
            stock.setSymbol(stockSymbol);
            stock.setCompany(company);
            stockRepository.save(stock);
            log.info("Stock " + stockSymbol + " added.");
        }
    };

    public String updateMacrotrendsTags() {
        Company company = new Company();
        String macrotrendsTag = "";
        for (Stock stock: stockRepository.findAll()) {
            company = stock.getCompany();
            macrotrendsTag = macrotrendsApiService.getMacrotrendsTag(stock);
            if (!macrotrendsTag.equals(company.getMacrotrendsTag())) {
                company.setMacrotrendsTag(macrotrendsTag);
                companyRepository.save(company);
                log.info(company.getName() + " - " + company.getMacrotrendsTag());
            }
        }
        log.info("Update Macrotrends tags was successfully finished.");
        return "OK";
    };

    public String updateMacrotrendsTag(String stockSymbol, String macrotrendsTag) {
        Optional<Stock> stock = stockRepository.findFirstBySymbol(stockSymbol);
        if (stock.isPresent()) {
            Company company = stock.get().getCompany();
            company.setMacrotrendsTag(macrotrendsTag);
            companyRepository.save(company);
            log.info("Macrotrends tag for " + stockSymbol + " was successfully updated to " + company.getMacrotrendsTag());
            return "OK";
        } else {
            log.info("Macrotrends tag update for " + stockSymbol + " caused error. Stock not found.");
            return "Error. Stock not found.";
        }
    };


}
