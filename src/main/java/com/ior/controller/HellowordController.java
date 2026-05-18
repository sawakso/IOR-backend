package com.ior.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HellowordController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }
}
