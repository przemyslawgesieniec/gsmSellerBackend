package pl.gesieniec.gsmseller.receipt;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.gesieniec.gsmseller.configuration.GlobalExceptionHandler;

class ReceiptEditValidationTest {
    @Test
    void rejectsInvalidDatesAmountsVatAndEmptyItemsBeforeCallingService() throws Exception {
        ReceiptEditService service = mock(ReceiptEditService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ReceiptEditController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        String valid = """
            {"generateDate":"2026-10-25","sellDate":"2026-10-24",
             "items":[{"id":1,"nettAmount":100.00,"vatRate":"23%"}]}
            """;
        for (String body : new String[] {
            valid.replace("2026-10-25", "bad-date"),
            valid.replace("100.00", "-1.00"), valid.replace("100.00", "1.001"),
            valid.replace("23%", "99%"), valid.replace("\"2026-10-25\"", "null"),
            valid.replace("1,\"nettAmount\"", "null,\"nettAmount\""),
            "{\"generateDate\":\"2026-10-25\",\"sellDate\":\"2026-10-24\",\"items\":[]}",
            "{\"generateDate\":\"2026-10-25\",\"sellDate\":\"2026-10-24\",\"items\":[null]}"
        }) {
            mvc.perform(post("/api/v1/receipt/00000000-0000-0000-0000-000000000001/edit?save=true")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
