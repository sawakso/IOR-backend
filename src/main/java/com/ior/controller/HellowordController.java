package com.ior.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "HelloWorldController", description = "HelloWorld控制器")
public class HellowordController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }
}
