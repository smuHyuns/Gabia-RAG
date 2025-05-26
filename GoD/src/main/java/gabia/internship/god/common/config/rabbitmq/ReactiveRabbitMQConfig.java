package gabia.internship.god.common.config.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.rabbitmq.*;

@Configuration
@RequiredArgsConstructor
public class ReactiveRabbitMQConfig {

    private final RabbitMQProperties props;

    @Bean
    public ConnectionFactory reactorConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(props.connection().host());
        factory.setUsername(props.connection().username());
        factory.setPassword(props.connection().password());
        factory.setVirtualHost("/");
        return factory;
    }

    @Bean
    public Sender sender(ConnectionFactory factory) {
        return RabbitFlux.createSender(new SenderOptions().connectionFactory(factory));
    }

    @Bean
    public Receiver receiver(ConnectionFactory factory) {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionFactory(factory));
    }
}

