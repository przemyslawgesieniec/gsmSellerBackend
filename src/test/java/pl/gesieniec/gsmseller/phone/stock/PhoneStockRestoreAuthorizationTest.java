package pl.gesieniec.gsmseller.phone.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(PhoneStockRestoreAuthorizationTest.Config.class)
class PhoneStockRestoreAuthorizationTest {

    @Autowired private PhoneStockController controller;
    @Autowired private PhoneStockService service;

    @BeforeEach
    void clearServiceInvocations() {
        clearInvocations(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToRestorePhone() {
        UUID technicalId = UUID.randomUUID();

        assertThat(controller.restorePhone(technicalId).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).restorePhone(technicalId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectNonAdministratorBeforeChangingPhone() {
        assertThatThrownBy(() -> controller.restorePhone(UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        PhoneStockService phoneStockService() {
            return mock(PhoneStockService.class);
        }

        @Bean
        PhoneStockController phoneStockController(PhoneStockService service) {
            return new PhoneStockController(service);
        }
    }
}
