package com.ior;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class IorApplication {

    public static void main(String[] args) {
        // 一行代码，自动加载 .env 文件并设置为系统属性，Spring Boot 会自动识别[citation:2]
        Dotenv.configure().ignoreIfMissing().systemProperties().load();

        SpringApplication.run(IorApplication.class, args);
    }
    @GetMapping("/test")
    public String test() {
        return "OK";
    }

}
