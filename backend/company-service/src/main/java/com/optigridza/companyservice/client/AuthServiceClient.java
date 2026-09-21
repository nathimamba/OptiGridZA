package com.optigridza.companyservice.client;

import com.optigridza.companyservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service", configuration = FeignClientConfig.class)
public interface AuthServiceClient {
    @PatchMapping("/api/v1/auth/users/{email}/company")
    void updateUserCompany(@PathVariable("email") String email, @RequestBody Map<String, String> body);
}
