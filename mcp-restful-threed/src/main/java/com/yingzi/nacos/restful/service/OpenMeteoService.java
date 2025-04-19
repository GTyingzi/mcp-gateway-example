package com.yingzi.nacos.restful.service;

import com.yingzi.nacos.restful.dto.WeatherData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author yingzi
 * @date 2025/4/6:13:05
 */
@Service
public class OpenMeteoService {
    private static final Logger logger = LoggerFactory.getLogger(OpenMeteoService.class);

    // OpenMeteo免费天气API基础URL
    private static final String BASE_URL = "https://api.open-meteo.com/v1";

    private final RestClient restClient;

    public OpenMeteoService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "OpenMeteoClient/1.0")
                .build();
    }

    public String getAirQuality(double latitude,
                               double longitude) {
        try {
            // 从天气数据中获取基本信息
            var weatherData = restClient.get()
                    .uri("/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m&timezone=auto",
                            latitude, longitude)
                    .retrieve()
                    .body(WeatherData.class);

            // 模拟空气质量数据 - 实际情况下应该从真实API获取
            // 根据经纬度生成一些随机但相对合理的数据
            int europeanAqi = (int) (Math.random() * 100) + 1;
            int usAqi = (int) (europeanAqi * 1.5);
            double pm10 = Math.random() * 50 + 5;
            double pm25 = Math.random() * 25 + 2;
            double co = Math.random() * 500 + 100;
            double no2 = Math.random() * 40 + 5;
            double so2 = Math.random() * 20 + 1;
            double o3 = Math.random() * 80 + 20;

            String aqiLevel = getAqiLevel(europeanAqi);
            String usAqiLevel = getUsAqiLevel(usAqi);

            // 构建空气质量信息字符串
            String aqiInfo = String.format("""
                    空气质量信息 (纬度: %.4f, 经度: %.4f, 时区: %s):

                    欧洲空气质量指数 (EAQI): %d (%s)
                    美国空气质量指数 (US AQI): %d (%s)

                    详细污染物信息:
                    PM10: %.1f μg/m³
                    PM2.5: %.1f μg/m³
                    一氧化碳 (CO): %.1f μg/m³
                    二氧化氮 (NO2): %.1f μg/m³
                    二氧化硫 (SO2): %.1f μg/m³
                    臭氧 (O3): %.1f μg/m³

                    注意：以上是模拟数据，仅供示例。
                    """,
                    latitude, longitude, weatherData.timezone(),
                    europeanAqi, aqiLevel,
                    usAqi, usAqiLevel,
                    pm10, pm25, co, no2, so2, o3);

            return aqiInfo;
        } catch (Exception e) {
            return "无法获取空气质量信息: " + e.getMessage();
        }
    }

    /**
     * 获取欧洲AQI等级描述
     */
    private String getAqiLevel(Integer aqi) {
        if (aqi <= 20) {
            return "优 (0-20): 空气质量非常好";
        } else if (aqi <= 40) {
            return "良 (20-40): 空气质量良好";
        } else if (aqi <= 60) {
            return "中等 (40-60): 对敏感人群可能有影响";
        } else if (aqi <= 80) {
            return "较差 (60-80): 对所有人群健康有影响";
        } else if (aqi <= 100) {
            return "差 (80-100): 可能对所有人群健康造成损害";
        } else {
            return "非常差 (>100): 对所有人群健康有严重影响";
        }
    }

    /**
     * 获取美国AQI等级描述
     */
    private String getUsAqiLevel(Integer aqi) {
        if (aqi <= 50) {
            return "优 (0-50): 空气质量令人满意，污染风险很低";
        } else if (aqi <= 100) {
            return "良 (51-100): 空气质量尚可，对极少数敏感人群可能有影响";
        } else if (aqi <= 150) {
            return "对敏感人群不健康 (101-150): 敏感人群可能会经历健康影响";
        } else if (aqi <= 200) {
            return "不健康 (151-200): 所有人可能开始经历健康影响";
        } else if (aqi <= 300) {
            return "非常不健康 (201-300): 健康警告，所有人可能经历更严重的健康影响";
        } else {
            return "危险 (>300): 健康警报，所有人更可能受到影响";
        }
    }

    public static void main(String[] args) {
        OpenMeteoService service = new OpenMeteoService();
        System.out.println(service.getAirQuality(39.9042, 116.4074));

    }
}
