package com.hookvault.analytics;

import com.hookvault.analytics.dto.AnalyticsDashboardResponse;
import com.hookvault.common.ApiResponse;
import com.hookvault.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AnalyticsDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AnalyticsDashboardResponse dashboard = analyticsService.getDashboard(principal);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}