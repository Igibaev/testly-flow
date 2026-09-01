package com.testlyflow.controller;

import com.testlyflow.dto.EmployeeCardDto;
import com.testlyflow.dto.EmployeeSummaryDto;
import com.testlyflow.service.AdminEmployeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {

    private final AdminEmployeeService adminEmployeeService;

    public AdminEmployeeController(AdminEmployeeService adminEmployeeService) {
        this.adminEmployeeService = adminEmployeeService;
    }

    @GetMapping
    public List<EmployeeSummaryDto> roster() {
        return adminEmployeeService.roster();
    }

    @GetMapping("/card")
    public EmployeeCardDto getCard(@RequestParam String firstName,
                                    @RequestParam String lastName,
                                    @RequestParam String team) {
        return adminEmployeeService.getCard(firstName, lastName, team);
    }
}
