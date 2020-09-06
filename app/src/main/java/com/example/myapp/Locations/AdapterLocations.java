package com.example.myapp.Locations;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.WeatherData.Main;
import com.example.myapp.WeatherData.Weather;
import com.example.myapp.WeatherData.WeatherData;
import com.example.myapp.WeatherService.OpenWeatherService;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// Адаптер
public class AdapterLocations extends RecyclerView.Adapter<AdapterLocations.ViewHolder> {

    private List<String> locations;
    private OpenWeatherService weatherService;

    public AdapterLocations(String[] locations) {
        this.locations = Arrays.asList(locations);
        weatherService = new OpenWeatherService();
    }

    @NonNull
    @Override
    public AdapterLocations.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_locations_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterLocations.ViewHolder viewHolder, int i) {
        viewHolder.setLocation(locations.get(i));
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView txtLocationName;
        private ImageView imgWeatherIcon;
        private TextView txtTemperature;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtLocationName = itemView.findViewById(R.id.txtLocationName);
            imgWeatherIcon = itemView.findViewById(R.id.imgWeatherIcon);
            txtTemperature = itemView.findViewById(R.id.txtTemperature);
        }

        public void setLocation(String location) {
            new Thread(() -> {
                AdapterData data = (AdapterData) weatherService.getData(location, AdapterData.class);
                String name = data.getLocationName();
                int res = data.getImageResource();
                String temp = data.getTemperature();
                txtLocationName.post(() -> txtLocationName.setText(data.getLocationName()));
                imgWeatherIcon.post(() -> imgWeatherIcon.setImageResource(data.getImageResource()));
                txtTemperature.post(() -> txtTemperature.setText(data.getTemperature()));
            }).start();
        }
    }

    private class AdapterData extends WeatherData {
        public Weather[] weather;
        public Main main;

        public AdapterData() {
            weather = new Weather[1];
        }

        public @NotNull
        String getLocationName() {
            if (requestIsFail()) return "";
            return name;
        }

        public int getImageResource() {
            if (requestIsFail()) return R.drawable.ic_report_problem;
            if (weather[0].icon.equals("01d")) return R.drawable.ic_01d;
            if (weather[0].icon.equals("02d")) return R.drawable.ic_02d;
            if (weather[0].icon.equals("03d")) return R.drawable.ic_03d;
            if (weather[0].icon.equals("04d")) return R.drawable.ic_04d;
            if (weather[0].icon.equals("09d")) return R.drawable.ic_09d;
            if (weather[0].icon.equals("10d")) return R.drawable.ic_10d;
            if (weather[0].icon.equals("11d")) return R.drawable.ic_11d;
            if (weather[0].icon.equals("13d")) return R.drawable.ic_13d;
            if (weather[0].icon.equals("50d")) return R.drawable.ic_50d;
            if (weather[0].icon.equals("01n")) return R.drawable.ic_01n;
            if (weather[0].icon.equals("02n")) return R.drawable.ic_02n;
            if (weather[0].icon.equals("03n")) return R.drawable.ic_03n;
            if (weather[0].icon.equals("04n")) return R.drawable.ic_04n;
            if (weather[0].icon.equals("09n")) return R.drawable.ic_09n;
            if (weather[0].icon.equals("10n")) return R.drawable.ic_10n;
            if (weather[0].icon.equals("11n")) return R.drawable.ic_11n;
            if (weather[0].icon.equals("13n")) return R.drawable.ic_13n;
            if (weather[0].icon.equals("50n")) return R.drawable.ic_50n;
            return R.drawable.ic_report_problem;
        }

        public @NotNull
        String getTemperature() {
            if (requestIsFail()) return "--°C";
            return String.format("%+.0f°C", main.temp);
        }

        private boolean requestIsFail() {
            return false;
        }

    }
}
