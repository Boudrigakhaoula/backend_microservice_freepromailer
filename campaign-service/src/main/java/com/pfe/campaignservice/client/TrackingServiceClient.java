package com.pfe.campaignservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Client Feign pour appeler le tracking-service via Eureka.
 */
@FeignClient(name = "tracking-service")
public interface TrackingServiceClient {

    @GetMapping("/api/tracking/stats/{campaignId}")
    Map<String, Object> getCampaignStats(@PathVariable("campaignId") String campaignId);
}
