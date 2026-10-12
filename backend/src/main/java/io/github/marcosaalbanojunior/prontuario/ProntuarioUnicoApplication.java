package io.github.marcosaalbanojunior.prontuario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProntuarioUnicoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProntuarioUnicoApplication.class, args);
    }
}
