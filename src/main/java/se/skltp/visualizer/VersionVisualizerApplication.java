package se.skltp.visualizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VersionVisualizerApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(VersionVisualizerApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
