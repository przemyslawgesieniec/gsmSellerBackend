package pl.gesieniec.gsmseller.receipt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ReceiptEditAuthorizationTest.Config.class)
class ReceiptEditAuthorizationTest {
    @Autowired ReceiptEditController controller;
    @Autowired ReceiptEditService service;

    @BeforeEach
    void clearCalls() { clearInvocations(service); }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanReadDownloadAndSave() {
        UUID id = UUID.randomUUID();
        controller.getForEditing(id);
        controller.editPdf(id, null, false);
        controller.editPdf(id, null, true);
        verify(service).getForEditing(id);
        verify(service).editPdf(id, null, false);
        verify(service).editPdf(id, null, true);
    }

    @Test
    @WithMockUser(roles = "USER")
    void employeeCannotReadDownloadOrSaveEvenViaDirectEndpoint() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> controller.getForEditing(id)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.editPdf(id, null, false)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.editPdf(id, null, true)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean ReceiptEditService service() { return mock(ReceiptEditService.class); }
        @Bean ReceiptEditController controller(ReceiptEditService service) { return new ReceiptEditController(service); }
    }
}
