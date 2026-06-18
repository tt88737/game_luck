package com.tangluck.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {
    @Test
    void mapsBusinessExceptionToApiError() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/test/region-blocked"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REGION_BLOCKED"))
                .andExpect(jsonPath("$.message").value("This feature is not available in your region."))
                .andExpect(jsonPath("$.trace_id").isNotEmpty())
                .andExpect(jsonPath("$.details.state_code").value("WA"));
    }

    @RestController
    public static class TestController {
        @GetMapping("/test/region-blocked")
        void regionBlocked() {
            throw new BusinessException(
                    ErrorCode.REGION_BLOCKED,
                    "This feature is not available in your region.",
                    Map.of("state_code", "WA")
            );
        }
    }
}
