package webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import redis.InfosWorker;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        try {
            new InfosWorker().run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
