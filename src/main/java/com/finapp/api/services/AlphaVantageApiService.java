package com.finapp.api.services;

import com.finapp.api.entity.Stock;
import com.finapp.api.entity.Quote;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlphaVantageApiService {

    private final RestTemplate restTemplate;

    @Value("${stocks-api-url}")
    private String stocksApiUrl;

    @Value("${stocks-api-key}")
    private String stocksApiKey;

    private final class StockQuoteDeserializer implements JsonDeserializer<Quote> {
        @Override
        public Quote deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            Quote result = new Quote();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            result.setOpen(jsonObject.get("1. open").getAsFloat());
            result.setHigh(jsonObject.get("2. high").getAsFloat());
            result.setLow(jsonObject.get("3. low").getAsFloat());
            result.setClose(jsonObject.get("4. close").getAsFloat());
            result.setVolume(jsonObject.get("5. volume").getAsLong());
            return result;
        }
    }

    private final class ListStockQuoteDeserializer implements JsonDeserializer<List<Quote>> {
        @Override
        public List<Quote> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            List<Quote> result = new LinkedList();
            LocalDate dateQuote;
            DateTimeFormatter dateFormat =DateTimeFormatter.ofPattern ("yyyy-MM-dd");
            JsonElement jsonPart = jsonElement.getAsJsonObject().get("Time Series (Daily)");
            if (jsonPart != null) {
                for (Map.Entry<String, JsonElement> entry: jsonPart.getAsJsonObject().entrySet()) {
                    Quote quote = jsonDeserializationContext.deserialize(entry.getValue(), Quote.class);
                    dateQuote = LocalDate.parse(entry.getKey(), dateFormat);
                    quote.setDate(dateQuote);
                    result.add(quote);
                }
            }
            return result;
        }
    }

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(new TypeToken<List<Quote>>() {}.getType(), new ListStockQuoteDeserializer())
            .registerTypeAdapter(Quote.class, new StockQuoteDeserializer())
            .create();

    // Get list of stock quotes for symbol <stock> after <dateLastQuote>
    public List<Quote> getListStockQuoteFromDate(Stock stock, LocalDate dateLastQuote) {
        String stocksApiSize = ChronoUnit.DAYS.between(dateLastQuote, LocalDate.now()) < 100 ? "compact" : "full";
        String response = restTemplate.getForObject(String.format(stocksApiUrl, stocksApiKey, stocksApiSize, stock.getSymbol()), String.class);
        List<Quote> result = gson.fromJson(response, new TypeToken<List<Quote>>() {}.getType());
        // Delete all records after <dateLastQuote>
        result.removeIf(item -> (ChronoUnit.DAYS.between(dateLastQuote, item.getDate()) < 1));
        // Set stock property for all records to <stock>
        result.forEach(item -> item.setStock(stock));
        return result;
    }

}
