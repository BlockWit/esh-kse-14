package com.blockwit.kafka.security.examples;

public class SimpleProducerTest_SASL_SSL_PLAIN {

    public static void main(String[] args) throws InterruptedException {
        SimpleProducer.runProducer(Helper.of(
                "sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required serviceName=\"Kafka\" username=\"alice\" password=\"alice-secret\";",
                "security.protocol", "SASL_PLAINTEXT",
                "sasl.mechanism", "PLAIN"),
                "localhost:9093", "test.topic");
    }

}
