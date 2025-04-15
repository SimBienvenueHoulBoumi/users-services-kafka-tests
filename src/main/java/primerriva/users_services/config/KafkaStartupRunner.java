package primerriva.users_services.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaStartupRunner implements CommandLineRunner {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaStartupRunner(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void run(String... args) {
        kafkaTemplate.send("user-microservices", "users microservices start successfully 🚀");
    }
}

