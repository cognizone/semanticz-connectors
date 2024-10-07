package zone.cogni.example.semanticz.connectors.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import zone.cogni.semanticz.connectors.spring.EnableSparqlServer;

@EnableSparqlServer("endpoint")
@SpringBootApplication
public class App {
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}