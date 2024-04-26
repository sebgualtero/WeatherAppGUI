//retrieve wheaterdata from API
//data from the external API and return it. The GUI will display this data to the user

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class WeatherApp {

    public static JSONObject getWeatherData(String locationName) {
        //get lcoation coodinates using the geolocation API
        JSONArray locationData = getLocationData(locationName);

        //extract latitude longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&timezone=America%2FLos_Angeles";

        try{
            //call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            //check response status
            if (conn.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API");
            }

            //store resulting json data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                //read and store into the string builder
                resultJson.append(scanner.nextLine());
            }

            scanner.close();

            conn.disconnect();

            //parse through our data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObject = (JSONObject) parser.parse(String.valueOf(resultJson));

            //retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObject.get("hourly");

            //load the current weather data
            JSONArray time = (JSONArray) hourly.get("time");

            //get index of the current hour to load everything else
            int index = findIndexOfCurrentTime(time);

            //extracting data
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            JSONArray weathercode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            JSONArray relativeHumidityData = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (long) relativeHumidityData.get(index);

            JSONArray windspeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windspeed = (double) windspeedData.get(index);

            JSONArray apparentTemperatureData = (JSONArray) hourly.get("apparent_temperature");
            double apparentTemperature = (double) apparentTemperatureData.get(index);

            //build Json object to feed the frontend
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("wind_speed", windspeed);
            weatherData.put("apparent_temperature", apparentTemperature);

            return weatherData;


        }catch(Exception e){
            e.printStackTrace();
        }

        return location;
    }

    //retrieves geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName) {
        //replace any whitespace in location name to + to adhere to API's URL request format
        locationName = locationName.replace(" ", "+");

        //build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + locationName + "&count=10&language=en&format=json";

        try{
            //call API and get a response
            HttpURLConnection conn = fetchApiResponse(urlString);

            //check response status
            //200 means successful connection
            if (conn.getResponseCode() != 200) {
                System.out.println("Error: " + conn.getResponseCode());
                return null;
            }else{
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                //read and store the resulting json data into our string builder
                while (scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }

                //close scanner
                scanner.close();

                //close url connection
                conn.disconnect();

                //parse the kson string into a json obj
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                //get the list of location data the API generated from the location name
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    };

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            //attempt to create a connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //set request method to get
            conn.setRequestMethod("GET");

            conn.connect();
            return conn;
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();

        for (int i = 0; i < timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                return i;
            }

        }
        return -1;
    }

    public static String getCurrentTime(){
        LocalDateTime currentDataTime = LocalDateTime.now();

        //format datetime to API format 2023-MM-DDT00:00
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        //format the current datetime
        String formattedDateTime = currentDataTime.format(formatter);

        return formattedDateTime;
    }

    private static String convertWeatherCode(long weathercode){
        String weatherCondition = "";

        if(weathercode == 0L){
            weatherCondition = "Clear";
        }else if (weathercode <= 3L && weathercode > 0L){
            weatherCondition = "Cloudy";
        }else if ((weathercode >= 51L && weathercode <= 61L) || (weathercode >= 80L && weathercode <= 99L)){
            weatherCondition = "Rain";
        } else if (weathercode >= 71L && weathercode <=77L) {
            weatherCondition = "Snow";
        }

        return weatherCondition;

    };
}
