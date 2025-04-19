package com.yingzi.nacos.restful.controller;

import com.yingzi.nacos.restful.service.OpenMeteoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * @author yingzi
 * @date 2025/4/6:13:00
 */
@RestController
@RequestMapping("/weather")
public class WeatherController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);


    @Autowired
    private OpenMeteoService openMeteoService;

    /**
     * 根据经纬度获取天气信息
     *
     * @param latitude  纬度
     * @param longitude 经度
     */
    @Operation(summary = "根据经纬度获取天气信息")
    @GetMapping("/city")
    public String getWeatherForecastByLocation(@Parameter(description = "纬度", required = true) @RequestParam("latitude") double latitude,
                                               @Parameter(description = "经度", required = true) @RequestParam("longitude") double longitude,
                                               HttpServletRequest request) {
        // 打印请求头信息
        for (String headerName : Collections.list(request.getHeaderNames())) {
            logger.info("Header {}: {}", headerName, request.getHeader(headerName));
        }

        logger.info("根据经纬度获取天气信息，纬度: {}, 经度: {}", latitude, longitude);
        return openMeteoService.getWeatherForecastByLocation(latitude, longitude);
    }
}
