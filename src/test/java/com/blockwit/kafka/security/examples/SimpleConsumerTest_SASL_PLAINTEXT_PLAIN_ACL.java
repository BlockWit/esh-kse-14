package com.blockwit.kafka.security.examples;

public class SimpleConsumerTest_SASL_PLAINTEXT_PLAIN_ACL {

    public static void main(String[] args) throws InterruptedException {
        SimpleConsumer.runConsumer(Helper.of(
                "sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required serviceName=\"Kafka\" username=\"robin\" password=\"robin-secret\";",
                "sasl.mechanism", "PLAIN",
                "security.protocol", "SASL_PLAINTEXT"),
                "localhost:9093", "test.topic", "test.group");
    }

}
