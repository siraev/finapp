package com.finapp.api.controllers;

import com.finapp.api.services.InitDataService;
import com.finapp.api.services.StockQuotesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StocksRestController {

    private final StockQuotesService stockQuotesService;
    private final InitDataService initDataService;

    // API request: /updateQuotes
    @GetMapping("updateQuotes")
    public String updateQuotes() {
        return stockQuotesService.updateQuotes();
    }

    // API request: /updateRatios
    @GetMapping("updateRatios")
    public String updateRatios() {
        return stockQuotesService.updateRatios();
    }

    // API request: /updateEarning
    @GetMapping("updateEarning")
    public String updateEarning() {
        return stockQuotesService.updateEps();
    }

    // API request: /updateExtremums
    @GetMapping("updateExtremums")
    public String updateExtremums() {
        return stockQuotesService.updateExtremums();
    }

    // API request: /loadStocksForSPB
    @GetMapping("loadStocksForSPB")
    public String loadStocksForSPB() {
        return initDataService.loadStocksForSPBExchange();
    }

    // API request: /updateMacrotrendsUrls
    @GetMapping("updateMacrotrendsTags")
    public String updateMacrotrendsTags() {
        return initDataService.updateMacrotrendsTags();
    }

    // API request: /updateMacrotrendsTag?symbol=XXX&tag=XXX
    @GetMapping("updateMacrotrendsTag")
    public String updateMacrotrendsUrl(@RequestParam("symbol") String stockSymbol, @RequestParam("tag") String macrotrendsTag) {
        return initDataService.updateMacrotrendsTag(stockSymbol, macrotrendsTag);
    }

    // API request: /updatePE
    @GetMapping("updatePE")
    public String updatePE() {
        return stockQuotesService.updatePE();
    }

    // API request: /getPE?symbol=XXX
    @GetMapping("getPE")
    public String getPE(@RequestParam("symbol") String stockSymbol) {
        return stockQuotesService.getPE(stockSymbol);
    }

}

/*    // API request: /addStock?symbol=XXX&company=XXX
    @GetMapping("addStock")
    public String addStock(@RequestParam("symbol") String stockSymbol, @RequestParam("company") String companyName) {
        return stockQuotesService.addStock(stockSymbol, companyName);
    }

    // API request: /getMinQuotes?symbol=XXX
    @GetMapping("getMinQuotes")
    public List<StockQuote> getMinQuotes(@RequestParam("symbol") String stockSymbol) {
        return stockQuotesService.getMinQuotes(stockSymbol);
    }*/

