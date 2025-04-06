package com.yingzi.nacos.restful.controller;

import com.yingzi.nacos.restful.utils.ZoneUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yingzi
 * @date 2025/4/6:12:56
 */
@RestController
@RequestMapping("/time")
public class TimeController {

    private static final Logger logger = LoggerFactory.getLogger(TimeController.class);


    /**
     * 获取指定时区的时间
     */
    @Operation(summary = "获取指定时区的时间")
    @GetMapping("/city")
    public String getCiteTimeMethod(
            @Parameter(description = "ime zone id, such as Asia/Shanghai", required = true)
            @RequestParam("timeZoneId") String timeZoneId) {
        logger.info("The current time zone is {}", timeZoneId);
        return String.format("The current time zone is %s and the current time is " + "%s", timeZoneId,
                ZoneUtils.getTimeByZoneId(timeZoneId));
    }
}
