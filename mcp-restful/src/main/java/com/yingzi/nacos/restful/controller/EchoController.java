package com.yingzi.nacos.restful.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yingzi
 * @date 2025/4/5:16:10
 * 测试网关正常
 */
@RestController
@RequestMapping("/echo")
public class EchoController {

    @GetMapping("/nacos")
    public String echo() {
        return "ok";
    }
}
