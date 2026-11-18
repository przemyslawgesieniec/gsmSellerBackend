package pl.gesieniec.gsmseller.repair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.configuration.GlobalExceptionHandler;
import pl.gesieniec.gsmseller.eventlog.EventLogRepository;
import pl.gesieniec.gsmseller.eventlog.EventLogService;
import pl.gesieniec.gsmseller.repair.client.RepairClient;
import pl.gesieniec.gsmseller.repair.client.RepairClientRepository;
import pl.gesieniec.gsmseller.repair.servicepoint.RepairServicePoint;
import pl.gesieniec.gsmseller.repair.servicepoint.RepairServicePointRepository;
import pl.gesieniec.gsmseller.repair.servicepoint.RepairServicePointService;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserRepository;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({RepairService.class, EventLogService.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RepairDeletionTest {

    @Autowired RepairService service;
    @Autowired RepairRepository repairs;
    @Autowired RepairNoteRepository notes;
    @Autowired EventLogRepository audit;
    @Autowired RepairClientRepository clients;
    @Autowired RepairServicePointRepository servicePoints;
    @Autowired UserRepository users;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean RepairMapper mapper;
    @MockitoBean RepairPdfService pdfService;
    @MockitoBean RepairHandoverPdfService handoverPdfService;
    @MockitoBean RepairServicePointService servicePointService;

    private static final String NOTE_CONTENT = "Dane naprawy ".repeat(150);

    private MockMvc mvc;
    private Repair repair;
    private RepairNote note;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new RepairController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        RepairClient client = clients.save(RepairClient.create("Jan", "Testowy", "600000000"));
        RepairServicePoint point = servicePoints.save(RepairServicePoint.create("Serwis " + UUID.randomUUID()));
        repair = Repair.create(client, "Apple", "iPhone", "123456789012345", "TELEFON", "czarny",
            "Rysa", "Nie włącza się", "Ładowarka", false, true, false, false, "1234",
            null, null, null, null, null, null, null, "RMA/1/" + UUID.randomUUID(),
            "Warszawa", "Wymiana baterii");
        repair.assignServicePoint(point);
        repairs.saveAndFlush(repair);

        User author = users.save(new User(UUID.randomUUID().toString(), "test-hash", "ROLE_SELLER"));
        note = notes.saveAndFlush(new RepairNote(repair, NOTE_CONTENT, author));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void rejectsSellerEvenWithCorrectRma() throws Exception {
        request(repair.getBusinessId()).andExpect(status().isForbidden());
        assertRepairIntact();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsMissingAndIncorrectRma() throws Exception {
        request(null).andExpect(status().isBadRequest());
        request(repair.getBusinessId().toLowerCase(java.util.Locale.ROOT)).andExpect(status().isBadRequest());
        assertRepairIntact();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deletesRepairAndNotesAndCommitsFullSnapshotExactlyOnce() throws Exception {
        request(repair.getBusinessId()).andExpect(status().isNoContent());
        request(repair.getBusinessId()).andExpect(status().isNotFound());

        assertThat(repairs.findByTechnicalId(repair.getTechnicalId())).isEmpty();
        assertThat(notes.existsById(note.getId())).isFalse();
        assertThat(repairs.findBusinessIdsByYear(repair.getBusinessId().split("/")[2]))
            .containsExactly(repair.getBusinessId());
        assertThat(clients.existsById(repair.getClient().getId())).isTrue();
        assertThat(servicePoints.existsById(repair.getServicePoint().getId())).isTrue();
        assertThat(audit.findAll()).filteredOn(entry -> repair.getTechnicalId().equals(entry.getResourceTechnicalId()))
            .singleElement().satisfies(entry -> {
                assertThat(entry.getActorUsername()).isEqualTo("admin");
                assertThat(entry.getDetails()).isEqualTo(repair.getBusinessId());
                var snapshot = objectMapper.readTree(entry.getDeletedRepairSnapshot());
                assertThat(snapshot.path("id").asLong()).isEqualTo(repair.getId());
                assertThat(snapshot.path("businessId").asText()).isEqualTo(repair.getBusinessId());
                assertThat(snapshot.path("lockCode").asText()).isEqualTo("1234");
                assertThat(snapshot.path("client").path("phoneNumber").asText()).isEqualTo("600000000");
                assertThat(snapshot.path("servicePoint").path("name").asText()).isEqualTo(repair.getServicePoint().getName());
                assertThat(snapshot.path("notes").get(0).path("content").asText()).isEqualTo(NOTE_CONTENT);
                assertThat(snapshot.path("notes").get(0).path("authorId").asLong()).isEqualTo(note.getAuthor().getId());
            });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rollsBackDeletionWhenAuditInsertFails() {
        jdbc.execute("""
            ALTER TABLE event_log ADD CONSTRAINT reject_delete_audit
            CHECK (deleted_repair_snapshot IS NULL OR resource_technical_id <> '%s')
            """.formatted(repair.getTechnicalId()));
        try {
            assertThatThrownBy(() -> service.deleteRepair(repair.getTechnicalId(), repair.getBusinessId()))
                .isInstanceOf(DataIntegrityViolationException.class);
            assertRepairIntact();
        } finally {
            jdbc.execute("ALTER TABLE event_log DROP CONSTRAINT reject_delete_audit");
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rollsBackAuditWhenRepairDeleteFails() {
        jdbc.execute("CREATE TABLE repair_delete_test_fk (id bigint REFERENCES repairs(id))");
        try {
            jdbc.update("INSERT INTO repair_delete_test_fk (id) VALUES (?)", repair.getId());
            assertThatThrownBy(() -> service.deleteRepair(repair.getTechnicalId(), repair.getBusinessId()))
                .isInstanceOf(DataIntegrityViolationException.class);
            assertRepairIntact();
        } finally {
            jdbc.execute("DROP TABLE repair_delete_test_fk");
        }
    }

    private void assertRepairIntact() {
        assertThat(repairs.existsById(repair.getId())).isTrue();
        assertThat(notes.existsById(note.getId())).isTrue();
        assertThat(audit.findAll()).noneMatch(entry -> repair.getTechnicalId().equals(entry.getResourceTechnicalId()));
    }

    private ResultActions request(String rma) throws Exception {
        var request = delete("/api/v1/repairs/{id}", repair.getTechnicalId()).accept("application/json");
        if (rma != null) {
            request.contentType("application/json").content(objectMapper.createObjectNode().put("rma", rma).toString());
        }
        return mvc.perform(request);
    }
}
