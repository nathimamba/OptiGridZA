package com.optigridza.etlservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.data-fetch.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:etl-test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS etl",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class EtlServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
