package pl.gesieniec.gsmseller.configuration;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringJUnitConfig(ModernFrontendSecurityTest.Config.class)
@WebAppConfiguration
class ModernFrontendSecurityTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test
    void loginAndAssetsArePublicButWorkspaceRequiresSession() throws Exception {
        mvc.perform(get("/v2/login.html")).andExpect(status().isOk());
        mvc.perform(get("/v2/assets/css/workspace.css")).andExpect(status().isOk());
        mvc.perform(get("/v2/index.html").servletPath("/v2/index.html")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/v2/login.html"));
        mvc.perform(get("/index.html")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login.html"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void employeeCanUseWorkspaceButCannotOpenInvoiceEditorOrItsApi() throws Exception {
        mvc.perform(get("/v2/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v2/invoice.html")).andExpect(status().isOk());
        mvc.perform(get("/v2/receipt-edit.html")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/receipt/11111111-1111-4111-8111-111111111111/edit"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanOpenBothEditorsAndBothFrontends() throws Exception {
        for (String path : new String[]{"/receipt-edit.html", "/v2/receipt-edit.html", "/index.html", "/v2/index.html"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class Config implements WebMvcConfigurer {
        @Bean UserDetailsService users() { return new InMemoryUserDetailsManager(); }
        @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        }
    }
}
