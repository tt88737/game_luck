package com.gameluck.web;

import com.gameluck.common.security.config.properties.SecurityProperties;
import com.gameluck.common.web.config.properties.XssProperties;
import com.gameluck.common.web.filter.XssFilter;
import com.gameluck.common.web.handler.GlobalExceptionHandler;
import com.gameluck.payment.controller.PaymentWebhookController;
import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.service.IPaymentWebhookService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("local")
class PaymentWebhookIngressSecurityTest {

    @Test
    void anonymousWebhookIsExcludedButOrdinaryPathRemainsProtected() throws Exception {
        SecurityProperties security = bind("security", SecurityProperties.class);
        assertEquals(1, Arrays.stream(security.getExcludes())
            .filter("/payment/webhooks/**"::equals).count());
        IPaymentWebhookService service = mock(IPaymentWebhookService.class);
        when(service.receive(anyString(), anyString(), anyString(), any()))
            .thenReturn(new PaymentWebhookAckVo("evt-1", "PROCESSED"));
        MockMvc mvc = mvc(service, security, null);

        mvc.perform(post("/payment/webhooks/SIMULATED")
                .with(request -> {
                    request.setServletPath("/payment/webhooks/SIMULATED");
                    return request;
                })
                .header("X-Payment-Timestamp", "100")
                .header("X-Payment-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk());
        mvc.perform(get("/protected/ping"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void xssExclusionPreservesExactWebhookHttpBodyBytes() throws Exception {
        SecurityProperties security = bind("security", SecurityProperties.class);
        XssProperties xss = bind("xss", XssProperties.class);
        assertEquals(1, xss.getExcludeUrls().stream()
            .filter("/payment/webhooks/**"::equals).count());
        IPaymentWebhookService service = mock(IPaymentWebhookService.class);
        when(service.receive(anyString(), anyString(), anyString(), any()))
            .thenReturn(new PaymentWebhookAckVo("evt-1", "PROCESSED"));
        byte[] raw = "  {\"note\":\"<keep-me>\"}\r\n".getBytes(StandardCharsets.UTF_8);
        MockMvc mvc = mvc(service, security, xss);

        mvc.perform(post("/payment/webhooks/SIMULATED")
                .with(request -> {
                    request.setServletPath("/payment/webhooks/SIMULATED");
                    return request;
                })
                .header("X-Payment-Timestamp", "100")
                .header("X-Payment-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(raw))
            .andExpect(status().isOk());

        verify(service).receive("SIMULATED", "100", "sig", raw);
    }

    @Test
    void invalidSignatureIsRealHttp401AndDoesNotPersist() throws Exception {
        SecurityProperties security = bind("security", SecurityProperties.class);
        IPaymentWebhookService service = mock(IPaymentWebhookService.class);
        when(service.receive(anyString(), anyString(), anyString(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment webhook"));
        MockMvc mvc = mvc(service, security, null);

        mvc.perform(post("/payment/webhooks/SIMULATED")
                .header("X-Payment-Timestamp", "100")
                .header("X-Payment-Signature", "not-returned")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void missingSignatureHeaderIsRealHttp400AndNeverCallsService() throws Exception {
        SecurityProperties security = bind("security", SecurityProperties.class);
        IPaymentWebhookService service = mock(IPaymentWebhookService.class);
        MockMvc mvc = mvc(service, security, null);

        mvc.perform(post("/payment/webhooks/SIMULATED")
                .header("X-Payment-Timestamp", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
        verify(service, never()).receive(anyString(), anyString(), anyString(), any());
    }

    private MockMvc mvc(IPaymentWebhookService service, SecurityProperties security,
                        XssProperties xss) throws Exception {
        var builder = MockMvcBuilders
            .standaloneSetup(new PaymentWebhookController(service), new ProtectedController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addInterceptors(authInterceptor(Arrays.asList(security.getExcludes())));
        if (xss != null) {
            XssFilter filter = new XssFilter();
            filter.excludes.addAll(xss.getExcludeUrls());
            builder.addFilters(filter);
        }
        return builder.build();
    }

    private HandlerInterceptor authInterceptor(List<String> excludes) {
        AntPathMatcher matcher = new AntPathMatcher();
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                boolean excluded = excludes.stream()
                    .anyMatch(pattern -> matcher.match(pattern, request.getRequestURI()));
                if (!excluded) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                }
                return excluded;
            }
        };
    }

    private <T> T bind(String prefix, Class<T> type) throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        List<PropertySource<?>> loaded = new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"));
        for (PropertySource<?> source : loaded) {
            sources.addFirst(source);
        }
        return Binder.get(environment).bind(prefix, type)
            .orElseThrow(() -> new IllegalStateException("Missing config: " + prefix));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/protected/ping")
        String ping() {
            return "pong";
        }
    }
}
