package com.pfe.campaignservice.client;


import com.pfe.campaignservice.dto.SendEmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Client Feign pour appeler le smtp-service via Eureka.
 * Correspond à l'endpoint POST /api/send du smtp-service.
 */
@FeignClient(name = "smtp-service")
public interface SmtpServiceClient {

    @PostMapping("/api/send")
    Map<String, String> sendEmail(@RequestBody SendEmailRequest request);

    @PostMapping("/api/send/campaign")
    Map<String, String> sendCampaignEmail(@RequestBody SendEmailRequest request);
}
