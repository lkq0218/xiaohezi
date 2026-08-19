package com.xiaohezi.api;

import com.xiaohezi.agent.AgentModels.ConfirmRequest;
import com.xiaohezi.agent.AgentModels.MessageRequest;
import com.xiaohezi.agent.AgentService;
import com.xiaohezi.config.SecurityProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AppController {
    private final AgentService agentService;
    private final ConcurrentWebService concurrentWebService;
    private final SecurityProperties securityProperties;

    public AppController(AgentService agentService, ConcurrentWebService concurrentWebService,
                         SecurityProperties securityProperties) {
        this.agentService = agentService;
        this.concurrentWebService = concurrentWebService;
        this.securityProperties = securityProperties;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("mode", "memory-demo");
        return ApiResponse.ok(data);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest body) {
        if ("oidc".equalsIgnoreCase(securityProperties.mode())) {
            return ResponseEntity.status(404)
                .body(ApiResponse.error("OIDC_LOGIN_REQUIRED", "请使用统一身份登录"));
        }
        if (!securityProperties.username().equals(body.username())
            || !securityProperties.password().equals(body.password())) {
            return ResponseEntity.status(401).body(ApiResponse.error("INVALID_CREDENTIALS", "账号或密码错误"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", securityProperties.token());
        data.put("tokenType", "Bearer");
        data.put("displayName", "演示用户");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/conversations")
    public ApiResponse<?> createConversation(Authentication auth) {
        return ApiResponse.ok(agentService.createConversation(auth.getName()));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<?>> message(@PathVariable String id,
                                                   @Valid @RequestBody MessageRequest request,
                                                   Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.handle(id, auth.getName(), request)));
    }

    @PostMapping("/actions/{token}/confirm")
    public ResponseEntity<ApiResponse<?>> confirm(@PathVariable String token,
                                                   @Valid @RequestBody ConfirmRequest request,
                                                   Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.confirm(token, auth.getName(), request.clientRequestId)));
    }

    @GetMapping("/bills")
    public ApiResponse<?> bills() {
        return ApiResponse.ok(agentService.currentBill());
    }

    @PostMapping("/learning/concurrency/fanout")
    public java.util.concurrent.CompletableFuture<ApiResponse<?>> fanout(
        @RequestBody Map<String, java.util.List<String>> body) {
        return concurrentWebService.fetch(body.get("urls"))
            .thenApply(result -> ApiResponse.ok(result));
    }

    public record LoginRequest(
        @NotBlank(message = "账号不能为空") @Size(max = 64) String username,
        @NotBlank(message = "密码不能为空") @Size(max = 128) String password
    ) {}
}
