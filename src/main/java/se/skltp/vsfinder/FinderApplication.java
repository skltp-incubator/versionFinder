package se.skltp.vsfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinderApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(FinderApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
