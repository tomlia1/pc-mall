package com.pcmall.controller;

import com.pcmall.common.Result;
import com.pcmall.dto.LoginRequest;
import com.pcmall.service.AdminService;
import com.pcmall.util.ClientIpResolver;
import com.pcmall.vo.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService adminService;
    public AdminController(AdminService adminService) { this.adminService = adminService; }
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(Result.success(adminService.login(request, ClientIpResolver.resolve(httpRequest))));
    }
}
