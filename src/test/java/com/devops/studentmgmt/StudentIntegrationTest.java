package com.devops.studentmgmt;

import com.devops.studentmgmt.model.Student;
import com.devops.studentmgmt.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests.
 *
 * Unlike StudentControllerTest (a @WebMvcTest slice that mocks the service) and
 * StudentServiceTest (pure Mockito), this test boots the real Spring context and
 * drives the whole stack: HTTP -> Controller -> Service -> JPA Repository -> H2.
 *
 * The database used here is the in-memory H2 configured in
 * src/test/resources/application.properties, so the real ./data/studentdb file
 * is never read or written by the test suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAll();
    }

    private Student sampleStudent() {
        return new Student("Priya Sharma", "priya@college.edu", "Data Science", 4);
    }

    private Student createStudent(Student student) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(student)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), Student.class);
    }

    @Nested
    @DisplayName("Health endpoint")
    class HealthTest {

        @Test
        @DisplayName("GET /actuator/health reports UP with the database component")
        void health_isUp() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
        }

        @Test
        @DisplayName("GET /actuator/info exposes the application metadata")
        void info_isExposed() throws Exception {
            mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Student Management System"))
                .andExpect(jsonPath("$.app.version").value("1.0.0"));
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTest {

        @Test
        @DisplayName("POST /api/students persists the student to H2 and returns 201")
        void create_persistsToDatabase() throws Exception {
            Student created = createStudent(sampleStudent());

            assertThat(created.getId()).isNotNull();
            assertThat(created.getFullName()).isEqualTo("Priya Sharma");
            assertThat(studentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("POST /api/students with a duplicate email returns 409")
        void create_duplicateEmail_returns409() throws Exception {
            createStudent(sampleStudent());

            mockMvc.perform(post("/api/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));

            assertThat(studentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("POST /api/students with an empty body returns 400 with per-field errors")
        void create_blankFields_returns400() throws Exception {
            Student invalid = new Student("", "not-an-email", "", null);

            mockMvc.perform(post("/api/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.department").exists())
                .andExpect(jsonPath("$.errors.semester").exists());

            assertThat(studentRepository.count()).isZero();
        }

        @Test
        @DisplayName("POST /api/students with semester 9 returns 400")
        void create_semesterTooHigh_returns400() throws Exception {
            Student invalid = new Student("Valid Name", "valid@college.edu", "IT", 9);

            mockMvc.perform(post("/api/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.semester").value("Semester cannot exceed 8"));
        }
    }

    @Nested
    @DisplayName("Read")
    class ReadTest {

        @Test
        @DisplayName("GET /api/students returns every stored student")
        void getAll_returnsAllStudents() throws Exception {
            createStudent(sampleStudent());
            createStudent(new Student("Ramesh Kumar", "ramesh@college.edu", "Computer Science", 3));

            mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fullName").value("Priya Sharma"))
                .andExpect(jsonPath("$[1].fullName").value("Ramesh Kumar"));
        }

        @Test
        @DisplayName("GET /api/students/{id} returns the matching student")
        void getById_returnsStudent() throws Exception {
            Student created = createStudent(sampleStudent());

            mockMvc.perform(get("/api/students/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.email").value("priya@college.edu"));
        }

        @Test
        @DisplayName("GET /api/students/{id} returns 404 when the student does not exist")
        void getById_missing_returns404() throws Exception {
            mockMvc.perform(get("/api/students/{id}", 999999))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/students/count returns the stored total")
        void count_returnsTotal() throws Exception {
            createStudent(sampleStudent());
            createStudent(new Student("Ramesh Kumar", "ramesh@college.edu", "Computer Science", 3));
            createStudent(new Student("Asha Patel", "asha@college.edu", "Mechanical Engineering", 6));

            mockMvc.perform(get("/api/students/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTest {

        @Test
        @DisplayName("PUT /api/students/{id} updates the stored student")
        void update_changesStoredStudent() throws Exception {
            Student created = createStudent(sampleStudent());
            Student changes = new Student("Priya S. Sharma", "priya@college.edu", "Artificial Intelligence", 5);

            mockMvc.perform(put("/api/students/{id}", created.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Priya S. Sharma"))
                .andExpect(jsonPath("$.department").value("Artificial Intelligence"))
                .andExpect(jsonPath("$.semester").value(5));

            Student stored = studentRepository.findById(created.getId()).orElseThrow();
            assertThat(stored.getFullName()).isEqualTo("Priya S. Sharma");
            assertThat(stored.getDepartment()).isEqualTo("Artificial Intelligence");
        }

        @Test
        @DisplayName("PUT /api/students/{id} allows keeping the same email on the same record")
        void update_sameEmailOnSameRecord_isAllowed() throws Exception {
            Student created = createStudent(sampleStudent());
            Student changes = new Student("Priya Sharma", "priya@college.edu", "Data Science", 7);

            mockMvc.perform(put("/api/students/{id}", created.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semester").value(7));
        }

        @Test
        @DisplayName("PUT /api/students/{id} returns 409 when another student holds the email")
        void update_emailTakenByAnother_returns409() throws Exception {
            createStudent(sampleStudent());
            Student second = createStudent(new Student("Ramesh Kumar", "ramesh@college.edu", "Computer Science", 3));
            Student clash = new Student("Ramesh Kumar", "priya@college.edu", "Computer Science", 4);

            mockMvc.perform(put("/api/students/{id}", second.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(clash)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already has email")));
        }

        @Test
        @DisplayName("PUT /api/students/{id} returns 404 when the student does not exist")
        void update_missing_returns404() throws Exception {
            mockMvc.perform(put("/api/students/{id}", 999999)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTest {

        @Test
        @DisplayName("DELETE /api/students/{id} removes the student from H2")
        void delete_removesStudent() throws Exception {
            Student created = createStudent(sampleStudent());

            mockMvc.perform(delete("/api/students/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student deleted successfully."));

            assertThat(studentRepository.existsById(created.getId())).isFalse();
            assertThat(studentRepository.count()).isZero();
        }

        @Test
        @DisplayName("DELETE /api/students/{id} returns 404 for an unknown id")
        void delete_missing_returns404() throws Exception {
            mockMvc.perform(delete("/api/students/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
        }
    }

    @Nested
    @DisplayName("Search")
    class SearchTest {

        @BeforeEach
        void seedStudents() throws Exception {
            createStudent(new Student("Priya Sharma", "priya@college.edu", "Data Science", 4));
            createStudent(new Student("Ramesh Kumar", "ramesh@college.edu", "Computer Science", 3));
            createStudent(new Student("Asha Patel", "asha@college.edu", "Computer Science", 6));
        }

        @Test
        @DisplayName("search matches by partial name, case-insensitively")
        void search_byName_isCaseInsensitive() throws Exception {
            mockMvc.perform(get("/api/students/search").param("query", "shArMa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Priya Sharma"));
        }

        @Test
        @DisplayName("search matches by partial department")
        void search_byDepartment() throws Exception {
            mockMvc.perform(get("/api/students/search").param("query", "Computer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("search with an empty query returns every student")
        void search_emptyQuery_returnsAll() throws Exception {
            mockMvc.perform(get("/api/students/search").param("query", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        }

        @Test
        @DisplayName("search with no match returns an empty list, not an error")
        void search_noMatch_returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/students/search").param("query", "zzz-not-found"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Repository persistence")
    class PersistenceTest {

        @Test
        @DisplayName("A student written by the repository is readable back from H2")
        void repositoryRoundTrip() {
            Student saved = studentRepository.save(new Student("Asha Patel", "asha@college.edu", "Electrical Engineering", 5));

            List<Student> found = studentRepository.searchByNameOrDepartment("Electrical");
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getId()).isEqualTo(saved.getId());
            assertThat(found.get(0).getFullName()).isEqualTo("Asha Patel");
            assertThat(found.get(0).getSemester()).isEqualTo(5);
        }

        @Test
        @DisplayName("Email uniqueness helpers agree with the stored data")
        void emailHelpers() {
            Student saved = studentRepository.save(new Student("Ramesh Kumar", "ramesh@college.edu", "Computer Science", 3));

            assertThat(studentRepository.existsByEmail("ramesh@college.edu")).isTrue();
            assertThat(studentRepository.existsByEmail("other@college.edu")).isFalse();
            assertThat(studentRepository.existsByEmailAndIdNot("ramesh@college.edu", saved.getId())).isFalse();
            assertThat(studentRepository.existsByEmailAndIdNot("ramesh@college.edu", 999999L)).isTrue();
        }
    }

    @Nested
    @DisplayName("Full lifecycle")
    class LifecycleTest {

        @Test
        @DisplayName("Create, read, update, delete and confirm the count returns to zero")
        void fullCrudRoundTrip() throws Exception {
            Student created = createStudent(sampleStudent());
            assertThat(studentRepository.count()).isEqualTo(1);

            mockMvc.perform(get("/api/students/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Priya Sharma"));

            Student updated = new Student("Priya Sharma", "priya@college.edu", "Artificial Intelligence", 6);
            mockMvc.perform(put("/api/students/{id}", created.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Artificial Intelligence"));

            mockMvc.perform(delete("/api/students/{id}", created.getId()))
                .andExpect(status().isOk());

            assertThat(studentRepository.count()).isZero();
            mockMvc.perform(get("/api/students/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        }
    }
}
