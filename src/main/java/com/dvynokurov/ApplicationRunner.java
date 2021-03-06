package com.dvynokurov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is a entry point of the backend part of the "Connect 4" game
 *
 * Game description can be found here: https://en.wikipedia.org/wiki/Connect_Four
 */
@SpringBootApplication
public class ApplicationRunner {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationRunner.class);
    }
}
