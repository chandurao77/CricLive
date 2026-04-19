package dev.cricklive.commentary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
public class CommentaryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentaryServiceApplication.class, args);
    }
}
