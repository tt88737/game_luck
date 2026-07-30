package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.payment.domain.bo.PaymentReconciliationResolutionBo;
import com.gameluck.payment.domain.vo.PaymentReconciliationIssueVo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class PaymentReconciliationControllerContractTest {

    @Test
    void exposesApprovedNineEndpointSurfaceWithExactPermissions() {
        Map<String, String> expected = Map.of(
            "list", "payment:reconciliation:list",
            "upload", "payment:reconciliation:upload",
            "detail", "payment:reconciliation:query",
            "lines", "payment:reconciliation:query",
            "issues", "payment:reconciliation:query",
            "execute", "payment:reconciliation:execute",
            "issueDetail", "payment:reconciliation:query",
            "resolve", "payment:reconciliation:resolve",
            "ignore", "payment:reconciliation:resolve");
        assertThat(PaymentReconciliationController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/payment/reconciliation");
        assertThat(PaymentReconciliationController.class.getDeclaredMethods())
            .filteredOn(method -> expected.containsKey(method.getName()))
            .hasSize(9)
            .allSatisfy(method -> assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly(expected.get(method.getName())));
    }

    @Test
    void mutationEndpointsAreOperationLoggedAndUploadUsesMultipartFilePart() throws Exception {
        for (String name : new String[]{"upload", "execute", "resolve", "ignore"}) {
            Method method = find(name);
            assertThat(method.getAnnotation(Log.class)).as(name).isNotNull();
        }
        for (String name : new String[]{"upload", "resolve", "ignore"}) {
            Log log = find(name).getAnnotation(Log.class);
            assertThat(log.isSaveRequestData()).as(name + " request").isFalse();
            assertThat(log.isSaveResponseData()).as(name + " response").isFalse();
        }
        Method upload = find("upload");
        Parameter file = java.util.Arrays.stream(upload.getParameters())
            .filter(p -> p.getType() == MultipartFile.class).findFirst().orElseThrow();
        assertThat(file.getAnnotation(RequestPart.class).value()).isEqualTo("file");
    }

    @Test
    void resolutionRequestRequiresNonNegativeExpectedVersionAndControllerValidation() throws Exception {
        assertThat(PaymentReconciliationResolutionBo.class.getDeclaredField("expectedVersion")
            .getAnnotation(NotNull.class)).isNotNull();
        assertThat(PaymentReconciliationResolutionBo.class.getDeclaredField("expectedVersion")
            .getAnnotation(Min.class).value()).isZero();
        assertThat(PaymentReconciliationIssueVo.class.getMethod("getVersion").getReturnType())
            .isEqualTo(Integer.class);
        for (String name : new String[]{"resolve", "ignore"}) {
            Parameter body = java.util.Arrays.stream(find(name).getParameters())
                .filter(parameter -> parameter.getType() == PaymentReconciliationResolutionBo.class)
                .findFirst().orElseThrow();
            assertThat(body.getAnnotation(Validated.class)).as(name).isNotNull();
        }
    }

    private static Method find(String name) {
        return java.util.Arrays.stream(PaymentReconciliationController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(name)).findFirst().orElseThrow();
    }
}
