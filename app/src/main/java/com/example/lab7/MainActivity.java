package com.example.lab7;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {
    private final String apiKey = "7e943c97096a9784391a981c4d878b22";
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(this);

        findViewById(R.id.buttonGetForecast).setOnClickListener(view -> {
            String cityName = ((TextView) findViewById(R.id.editTextCity)).getText().toString().trim();
            if (cityName.isEmpty()) {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                cityName = URLEncoder.encode(cityName, "UTF-8");
                String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName +
                        "&appid=" + apiKey + "&units=metric";
                fetchWeatherData(url);
            } catch (Exception e) {
                Toast.makeText(this, "Error encoding city name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchWeatherData(String url) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject main = response.getJSONObject("main");
                        double currentTemp = main.getDouble("temp");
                        double maxTemp = main.getDouble("temp_max");
                        double minTemp = main.getDouble("temp_min");
                        int humidity = main.getInt("humidity");

                        JSONArray weatherArray = response.getJSONArray("weather");
                        JSONObject weather = weatherArray.getJSONObject(0);
                        String description = weather.getString("description");
                        String icon = weather.getString("icon");

                        updateUI(currentTemp, maxTemp, minTemp, humidity, description, icon);
                    } catch (JSONException e) {
                        Toast.makeText(this, "JSON Parsing error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Request failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());

        queue.add(request);
    }

    private void updateUI(double currentTemp, double maxTemp, double minTemp, int humidity, String description, String icon) {
        runOnUiThread(() -> {
            ((TextView) findViewById(R.id.textCurrentTemp)).setText("Current Temp: " + currentTemp + "°C");
            ((TextView) findViewById(R.id.textMaxTemp)).setText("Max Temp: " + maxTemp + "°C");
            ((TextView) findViewById(R.id.textMinTemp)).setText("Min Temp: " + minTemp + "°C");
            ((TextView) findViewById(R.id.textHumidity)).setText("Humidity: " + humidity + "%");
            ((TextView) findViewById(R.id.textDescription)).setText("Description: " + description);

            setVisibility(true);

            fetchWeatherIcon(icon);
        });
    }

    private void setVisibility(boolean visible) {
        int visibility = visible ? TextView.VISIBLE : TextView.GONE;
        findViewById(R.id.textCurrentTemp).setVisibility(visibility);
        findViewById(R.id.textMaxTemp).setVisibility(visibility);
        findViewById(R.id.textMinTemp).setVisibility(visibility);
        findViewById(R.id.textHumidity).setVisibility(visibility);
        findViewById(R.id.textDescription).setVisibility(visibility);
        findViewById(R.id.imageWeatherIcon).setVisibility(visibility);
    }

    private void fetchWeatherIcon(String icon) {
        String fileName = icon + ".png";
        File file = new File(getFilesDir(), fileName);

        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Bitmap bitmap = BitmapFactory.decodeStream(fis);
                ((ImageView) findViewById(R.id.imageWeatherIcon)).setImageBitmap(bitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Error loading cached icon", Toast.LENGTH_SHORT).show();
            }
        } else {
            String iconUrl = "https://openweathermap.org/img/w/" + icon + ".png";
            ImageRequest imageRequest = new ImageRequest(iconUrl, bitmap -> {
                ((ImageView) findViewById(R.id.imageWeatherIcon)).setImageBitmap(bitmap);
                saveBitmapToFile(file, bitmap);
            }, 0, 0, ImageView.ScaleType.CENTER, null,
                    error -> Toast.makeText(this, "Failed to load icon", Toast.LENGTH_SHORT).show());

            queue.add(imageRequest);
        }
    }

    private void saveBitmapToFile(File file, Bitmap bitmap) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            Toast.makeText(this, "Error saving icon", Toast.LENGTH_SHORT).show();
        }
    }
}
