package com.haomei.haomei.controller.admin;

import com.haomei.haomei.dto.TableSessionResponse;
import com.haomei.haomei.service.TableSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tables")
public class AdminTableController {

    private final TableSessionService tableSessionService;

    public AdminTableController(TableSessionService tableSessionService) {
        this.tableSessionService = tableSessionService;
    }

    @GetMapping
    public List<TableSessionResponse> list(@RequestParam(required = false) String baseUrl) {
        return tableSessionService.listAll(baseUrl);
    }
}

