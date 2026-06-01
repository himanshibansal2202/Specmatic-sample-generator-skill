package com.example.orderapi;

import io.specmatic.test.SpecmaticContractTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.stream.Stream;

public class ContractTest implements SpecmaticContractTest {

    private static ConfigurableApplicationContext context;

    @BeforeAll
    static void setUp() {
        context = SpringApplication.run(OrderApiApplication.class);
    }

    @AfterAll
    static void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Override
    @TestFactory
    public Stream<DynamicTest> testStream() {
        return SpecmaticContractTest.super.testStream();
    }
}
