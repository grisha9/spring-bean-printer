package org.springframework.sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@SpringBootApplication(scanBasePackages = "org.springframework.sample")
public class Application {
    @Autowired
    TestComponentBean componentBean;
    @Autowired
    TestMethodBean methodBean;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Component
class TestComponentBean {
}

@Configuration
class TestConfig {
    @Bean
    public TestMethodBean methodBean() {
        return new TestMethodBean();
    }
}

class TestMethodBean {
}