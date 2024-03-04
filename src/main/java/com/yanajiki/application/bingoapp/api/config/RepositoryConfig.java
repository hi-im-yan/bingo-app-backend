package com.yanajiki.application.bingoapp.api.config;

import com.yanajiki.application.bingoapp.domain.service.RoomRepositoryPort;
import com.yanajiki.application.bingoapp.infra.database.RoomJpaRepository;
import com.yanajiki.application.bingoapp.infra.database.RoomRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class RepositoryConfig {

    private final RoomRepository repository;

    @Bean
    public RoomRepositoryPort roomRepository() {
        return new RoomJpaRepository(repository);
    }
}
