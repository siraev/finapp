package com.finapp.api.config;

import com.finapp.api.services.StockQuotesService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final StockQuotesService stockQuotesService;

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledStockQuotesUpdate() {
        log.info("Start scheduled tasks");
        stockQuotesService.updateQuotes();
        log.info("Scheduled tasks finished");
    }
}
