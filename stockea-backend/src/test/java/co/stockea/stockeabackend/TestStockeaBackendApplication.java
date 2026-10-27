package co.stockea.stockeabackend;

import co.stockea.StockeaBackendApplication;
import org.springframework.boot.SpringApplication;

public class TestStockeaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(StockeaBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
