package com.back;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection // 컨테이너의 주소·계정을 spring.datasource.* 대신 채워 준다
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName
                .parse("pgvector/pgvector:pg17")              // pgvector 확장이 들어 있는 PostgreSQL 17
                .asCompatibleSubstituteFor("postgres"))
                .withInitScript("init-pgvector.sql");          // 시작 시 CREATE EXTENSION vector 실행
    }
}
