package com.tangluck.common;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {
    @Test
    void rootReturnsServiceStatus() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tang Luck P0-A")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://127.0.0.1:5175/app/register")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://127.0.0.1:5175/admin")));
    }

    @Test
    void healthReturnsOk() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
