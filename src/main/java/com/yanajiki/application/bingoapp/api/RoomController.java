package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.api.form.RoomDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/room")
public class RoomController {

    @PostMapping
    public RoomDTO create(@RequestBody RoomDTO input) {

    }
}
