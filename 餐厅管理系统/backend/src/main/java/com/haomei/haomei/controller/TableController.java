package com.haomei.haomei.controller;

import com.haomei.haomei.dto.TableResolveResponse;
import com.haomei.haomei.service.TableSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableSessionService tableSessionService;

    public TableController(TableSessionService tableSessionService) {
        this.tableSessionService = tableSessionService;
    }

    @GetMapping("/resolve")
    public TableResolveResponse resolve(@RequestParam String token) {
        return tableSessionService.resolve(token);
    }
}

