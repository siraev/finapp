package com.finapp.api.services;

import com.finapp.api.entity.Ratio;
import com.finapp.api.entity.Stock;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MacrotrendsApiService {

    private static Logger log = LoggerFactory.getLogger(StockQuotesService.class);

    private final RestTemplate restTemplate;

    enum RatioType{
        EPS,
        BPS,
        CUR_RATIO,
        DEBT_EQUITY,
        ROE,
        ROA,
        ROI
    }

    @Value("${macrotrends-api-url}")
    private String macrotrendsApiUrl;

    @Value("${macrotrends-search-url}")
    private String macrotrendsSearchUrl;

    @Value("${macrotrends-eps-url}")
    private String macrotrendsEpsUrl;

    @Value("${macrotrends-bps-url}")
    private String macrotrendsBpsUrl;

    @Value("${macrotrends-cur-ratio-url}")
    private String macrotrendsCurRatioUrl;

    @Value("${macrotrends-debt-equity-url}")
    private String macrotrendsDebtEquityUrl;

    @Value("${macrotrends-roe-url}")
    private String macrotrendsRoeUrl;

    @Value("${macrotrends-roa-url}")
    private String macrotrendsRoaUrl;

    @Value("${macrotrends-roi-url}")
    private String macrotrendsRoiUrl;

    @Value("${macrotrends-ratios-url}")
    private String macrotrendsRatiosUrl;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(new TypeToken<List<String>>() {}.getType(), new MacrotrendsApiService.MacrotrendsTagDeserializer())
            .create();

    private final class MacrotrendsTagDeserializer implements JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            List<String> result = new ArrayList<>();
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            if (jsonArray.size() > 0) {
                result.add(jsonArray.get(0).getAsJsonObject().get("name").getAsString());
                result.add(jsonArray.get(0).getAsJsonObject().get("url").getAsString());
            }
            return result;
        }
    }

    // Search urls for stock
    public String getMacrotrendsTag(Stock stock) {
        String result = searchMacrotrendsTag(stock.getSymbol(), stock.getSymbol());
        if (result.equals("")) {
            for (String word: stock.getCompany().getName().split(" ")) {
                result = searchMacrotrendsTag(word, stock.getSymbol());
                if (!result.equals("")) {
                    break;
                }
            }
        }
        return result;
    }

    private String searchMacrotrendsTag(String searchString, String stockSymbol) {
        String result = "";
        String response = restTemplate.getForObject(String.format(macrotrendsApiUrl + macrotrendsSearchUrl, searchString), String.class);
        if (!response.equals("null")) {
            List<String> list = gson.fromJson(response, new TypeToken<List<String>>() {}.getType());
            if ((list.size() > 0) && (list.get(0).contains(stockSymbol))) {
                result = list.get(1).split("/")[4];
            }
        }
        return result;
    }

    public HashMap<Long, Float> getAnnualEps(Stock stock) {
        HashMap<Long, Float> result = new HashMap<>();
        try {
            String macrotrendsTag = stock.getCompany().getMacrotrendsTag();
            if ((macrotrendsTag != null) && (!macrotrendsTag.equals(""))) {
                Document doc = Jsoup.connect(String.format(macrotrendsApiUrl + macrotrendsEpsUrl, stock.getSymbol(), macrotrendsTag)).get();
                Element table = doc.getElementsByClass("historical_data_table table").first();
                if (table != null) {
                    Elements elems = table.selectFirst("tbody").select("tr");
                    for (Element entry : elems) {
                        Long year = Long.parseLong(entry.select("td").get(0).text());
                        Float eps = Float.parseFloat(entry.select("td").get(1).text().replaceAll("[^\\d.]+", ""));
                        result.put(year, eps);
                    }
                }
            }
        } catch (Exception e) {
            log.info(e.getLocalizedMessage(), e);
            return null;
        }
        return result;
    };

    public HashMap<LocalDate, Float> getQuarterlyEps(Stock stock) {
        HashMap<LocalDate, Float> result = new HashMap<>();
        try {
            String macrotrendsTag = stock.getCompany().getMacrotrendsTag();
            if ((macrotrendsTag != null) && (!macrotrendsTag.equals(""))) {
                Document doc = Jsoup.connect(String.format(macrotrendsApiUrl + macrotrendsEpsUrl, stock.getSymbol(), macrotrendsTag)).get();
                Elements tables = doc.getElementsByClass("historical_data_table table");
                if (tables != null && tables.size() > 1) {
                    Element table = tables.get(1);
                    if (table != null) {
                        Elements elems = table.selectFirst("tbody").select("tr");
                        for (Element entry : elems) {
                            LocalDate date = LocalDate.parse(entry.select("td").get(0).text(), DateTimeFormatter.ISO_LOCAL_DATE);
                            Float eps = Float.parseFloat(entry.select("td").get(1).text().replaceAll("[^\\d.]+", ""));
                            result.put(date, eps);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info(e.getLocalizedMessage(), e);
            return null;
        }
        return result;
    };

    private Optional<Float> getRatioTableValue(String ratioTableUrl, String stockSymbol, String macrotrendsTag, int columnIndex) {
        try {
            Document doc = Jsoup.connect(String.format(macrotrendsApiUrl + ratioTableUrl, stockSymbol, macrotrendsTag)).get();
            Element table = doc.getElementsByClass("table").first();
            if (table != null) {
                Elements elems = table.selectFirst("tbody").select("tr");
                for (Element entry : elems) {
                    LocalDate date = LocalDate.parse(entry.select("td").get(0).text());
                    String cell = entry.select("td").get(columnIndex).text();
                    if (cell != null && !cell.equals("") && !cell.replaceAll("[^\\d.]+", "").equals("")) {
                        return Optional.of(Float.parseFloat(cell.replaceAll("[^\\d.]+", "")));
                    }
                }
            }
        } catch (Exception e) {
            log.info(e.getLocalizedMessage(), e);
            return Optional.empty();
        }
        return Optional.empty();
    };

    public Optional<Float> getRatio(RatioType ratioType, Stock stock) {
        Optional<Float> result = Optional.empty();
        String macrotrendsTag = stock.getCompany().getMacrotrendsTag();
        if ((macrotrendsTag != null) && (!macrotrendsTag.equals(""))) {
            switch (ratioType) {
                case BPS:
                    result = getRatioTableValue(macrotrendsBpsUrl, stock.getSymbol(), macrotrendsTag, 2);
                    break;
                case CUR_RATIO:
                    result = getRatioTableValue(macrotrendsCurRatioUrl, stock.getSymbol(), macrotrendsTag, 3);
                    break;
                case DEBT_EQUITY:
                    result = getRatioTableValue(macrotrendsDebtEquityUrl, stock.getSymbol(), macrotrendsTag, 3);
                    break;
                case ROE:
                    result = getRatioTableValue(macrotrendsRoeUrl, stock.getSymbol(), macrotrendsTag, 3);
                    break;
                case ROA:
                    result = getRatioTableValue(macrotrendsRoaUrl, stock.getSymbol(), macrotrendsTag, 3);
                    break;
                case ROI:
                    result = getRatioTableValue(macrotrendsRoiUrl, stock.getSymbol(), macrotrendsTag, 3);
                    break;
            }
        }
        return result;
    }

}


/*
            result.setBps(ratio.isPresent() ? ratio.get(): null);
            ratio = getRatioTableValue(macrotrendsCurRatioUrl, stock.getSymbol(), macrotrendsTag, 3);
            result.setCurrentRatio(ratio.isPresent() ? ratio.get(): null);
            ratio = getRatioTableValue(macrotrendsDebtEquityUrl, stock.getSymbol(), macrotrendsTag, 3);
            result.setDebtToEquity(ratio.isPresent() ? ratio.get(): null);
            ratio = getRatioTableValue(macrotrendsRoeUrl, stock.getSymbol(), macrotrendsTag, 3);
            result.setRoe(ratio.isPresent() ? ratio.get(): null);
            ratio = getRatioTableValue(macrotrendsRoaUrl, stock.getSymbol(), macrotrendsTag, 3);
            result.setRoa(ratio.isPresent() ? ratio.get(): null);
            ratio = getRatioTableValue(macrotrendsRoiUrl, stock.getSymbol(), macrotrendsTag, 3);
            result.setRoi(ratio.isPresent() ? ratio.get(): null);

                Document doc = Jsoup.connect(String.format(macrotrendsApiUrl + macrotrendsRatiosUrl, stock.getSymbol(), macrotrendsTag))
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36")
                        .cookie("_ga=GA1.2.1914471673.1582699911; _jsuid=1313145854; __qca=P0-1315887558-1582699911486; kppid_managed=Mm1Dp4BT; _referrer_og=https%3A%2F%2Fwww.google.com%2F; _gid=GA1.2.208440516.1584368658","")
                        .referrer(macrotrendsApiUrl)
                        .maxBodySize(0)
                        .timeout(0)
                        .get();
                Elements table = doc.getElementById("contenttablejqxgrid").children();*/
