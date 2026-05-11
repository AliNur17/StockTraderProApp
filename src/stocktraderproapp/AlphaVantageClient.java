package stocktraderproapp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class AlphaVantageClient {

    private final String apiKey;

    public AlphaVantageClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public JSONObject fetchDailyTimeSeries(String symbol) throws Exception {

        String urlString =
                "https://www.alphavantage.co/query?"
                + "function=TIME_SERIES_DAILY"
                + "&symbol=" + symbol
                + "&outputsize=compact"
                + "&apikey=" + apiKey;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        JSONObject json = new JSONObject(response.toString());

        if (!json.has("Time Series (Daily)")) {

            if (json.has("Information")) {
                throw new Exception(json.getString("Information"));
            }

            if (json.has("Note")) {
                throw new Exception(json.getString("Note"));
            }

            throw new Exception(
                    "No stock data found for " + symbol
                    + ". Check the symbol, API key, or request limit."
            );
        }

        return json.getJSONObject("Time Series (Daily)");
    }
}
