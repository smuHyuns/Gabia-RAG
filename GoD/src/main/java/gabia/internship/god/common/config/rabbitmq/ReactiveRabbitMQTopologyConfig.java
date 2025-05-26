package gabia.internship.god.common.config.rabbitmq;

import gabia.internship.god.common.constants.Constants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveRabbitMQTopologyConfig {

    private final Sender sender;

    private final RabbitMQProperties props;

    @PostConstruct
    public void init() {
        declareTopology().subscribe(
                null,
                error -> log.error("토폴로지 선언 중 오류 발생", error),
                () -> log.info("모든 큐/익스체인지/바인딩 선언 완료")
        );
    }

    private Mono<Void> declareTopology() {
        return Mono.when(
                // Exchanges
                sender.declareExchange(ExchangeSpecification.exchange(props.exchange().main()).type("direct").durable(true)),
                sender.declareExchange(ExchangeSpecification.exchange(props.exchange().dead()).type("direct").durable(true)),

                // Queues
                declareQueueWithDLQ(props.queue().workEmbedding(), 500_000),
                declareQueueWithDLQ(props.queue().workEmbeddingCsv(), 1_000_000),
                declareQueueWithDLQ(props.queue().workSearch(), 10_000),
                declareQueueWithDLQ(props.queue().workGenerate(), 10_000),
                declareQueueWithDLQ(props.queue().workDocument(), 1_000_000),
                declareQueueWithDLQ(props.queue().workDocumentMail(), 1_000_000),
                declareQueueWithDLQ(props.queue().workDocumentParsing(), 1_000_000),

                // 응답 큐 (DLX 없이)
                sender.declareQueue(QueueSpecification.queue(props.queue().workResponse()).durable(true)),

                // Dead queue
                sender.declareQueue(QueueSpecification.queue(props.queue().dead()).durable(true)),

                // Bindings
                bind(props.queue().workEmbedding(), props.exchange().main(), props.routing().workEmbedding()),
                bind(props.queue().workEmbeddingCsv(), props.exchange().main(), props.routing().workEmbeddingCsv()),
                bind(props.queue().workSearch(), props.exchange().main(), props.routing().workSearch()),
                bind(props.queue().workGenerate(), props.exchange().main(), props.routing().workGenerate()),
                bind(props.queue().workDocument(), props.exchange().main(), props.routing().workDocument()),
                bind(props.queue().workDocumentMail(), props.exchange().main(), props.routing().workDocumentMail()),
                bind(props.queue().workDocumentParsing(), props.exchange().main(), props.routing().workDocumentParsing()),
                bind(props.queue().workResponse(), props.exchange().main(), props.routing().workResponse()),

                // Dead queue binding
                bind(props.queue().dead(), props.exchange().dead(), props.routing().dead())
        );
    }

    private Mono<Void> declareQueueWithDLQ(String queueName, int ttlMs) {
        Map<String, Object> args = new HashMap<>();
        args.put(Constants.RABBITMQ_MESSAGE_HEADER_TTL, ttlMs);
        args.put(Constants.RABBITMQ_MESSAGE_HEADER_DLX, props.exchange().dead());
        args.put(Constants.RABBITMQ_MESSAGE_HEADER_DLX_ROUTING, props.routing().dead());

        return sender.declareQueue(QueueSpecification.queue(queueName)
                .durable(true)
                .arguments(args)).then();
    }

    private Mono<Void> bind(String queue, String exchange, String routingKey) {
        BindingSpecification spec = new BindingSpecification();
        spec.queue(queue);
        spec.exchange(exchange);
        spec.routingKey(routingKey);
        return sender.bind(spec).then();
    }

}
