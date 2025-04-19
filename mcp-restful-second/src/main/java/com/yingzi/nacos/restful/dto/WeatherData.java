package com.yingzi.nacos.restful.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author yingzi
 * @date 2025/4/6:13:10
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherData(
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("timezone") String timezone,
        @JsonProperty("current") CurrentWeather current,
        @JsonProperty("daily") DailyForecast daily,
        @JsonProperty("current_units") CurrentUnits currentUnits) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentWeather(
            @JsonProperty("time") String time,
            @JsonProperty("temperature_2m") Double temperature,
            @JsonProperty("apparent_temperature") Double feelsLike,
            @JsonProperty("relative_humidity_2m") Integer humidity,
            @JsonProperty("precipitation") Double precipitation,
            @JsonProperty("weather_code") Integer weatherCode,
            @JsonProperty("wind_speed_10m") Double windSpeed,
            @JsonProperty("wind_direction_10m") Integer windDirection) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentUnits(
            @JsonProperty("time") String timeUnit,
            @JsonProperty("temperature_2m") String temperatureUnit,
            @JsonProperty("relative_humidity_2m") String humidityUnit,
            @JsonProperty("wind_speed_10m") String windSpeedUnit) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DailyForecast(
            @JsonProperty("time") List<String> time,
            @JsonProperty("temperature_2m_max") List<Double> tempMax,
            @JsonProperty("temperature_2m_min") List<Double> tempMin,
            @JsonProperty("precipitation_sum") List<Double> precipitationSum,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("wind_speed_10m_max") List<Double> windSpeedMax,
            @JsonProperty("wind_direction_10m_dominant") List<Integer> windDirection) {
    }
}
