package com.example.social;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
        "DB_MIGRATION_ENABLED=false",
        "app.storage.s3.bucket=test-bucket",
        "app.storage.s3.region=ap-southeast-2"
    }
)
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
