package com.yanajiki.application.bingoapp.api.config;

import com.yanajiki.application.bingoapp.domain.service.IRoomService;
import com.yanajiki.application.bingoapp.domain.service.RoomService;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    public IRoomService roomService() {
        return new RoomService();
    }
}
