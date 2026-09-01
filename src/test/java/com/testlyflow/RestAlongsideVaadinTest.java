package com.testlyflow;

import com.testlyflow.dto.CategoryDto;
import com.testlyflow.service.AdminAttemptService;
import com.testlyflow.service.AdminEmployeeService;
import com.testlyflow.service.AdminTestService;
import com.testlyflow.service.AttemptService;
import com.testlyflow.service.CategoryService;
import com.testlyflow.service.FeedbackService;
import com.testlyflow.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "vaadin.launch-browser=false"
})
@AutoConfigureMockMvc
class RestAlongsideVaadinTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean CategoryService categoryService;
    @MockBean AttemptService attemptService;
    @MockBean AdminTestService adminTestService;
    @MockBean AdminAttemptService adminAttemptService;
    @MockBean AdminEmployeeService adminEmployeeService;
    @MockBean MetricsService metricsService;
    @MockBean FeedbackService feedbackService;

    @Test
    void categoriesApiStillReturnsJsonNotVaadinHtml() throws Exception {
        when(categoryService.listPublic()).thenReturn(List.of(
                new CategoryDto(1L, "Флоу", null, "#c2410c", 3, List.of())));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Флоу"));
    }
}
