package com.devops.studentmgmt;

import com.devops.studentmgmt.controller.StudentController;
import com.devops.studentmgmt.model.Student;
import com.devops.studentmgmt.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

    private Student sampleStudent;

    @BeforeEach
    void setUp() {
        sampleStudent = new Student("Priya Sharma", "priya@college.edu", "Data Science", 4);
        sampleStudent.setId(1L);
    }

    @Test
    @DisplayName("GET /api/students - returns 200 with list")
    void getAllStudents_returns200() throws Exception {
        when(studentService.getAllStudents()).thenReturn(List.of(sampleStudent));
        mockMvc.perform(get("/api/students"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fullName").value("Priya Sharma"))
            .andExpect(jsonPath("$[0].department").value("Data Science"));
    }

    @Test
    @DisplayName("GET /api/students/{id} - returns 200 when found")
    void getStudentById_found_returns200() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(Optional.of(sampleStudent));
        mockMvc.perform(get("/api/students/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Priya Sharma"));
    }

    @Test
    @DisplayName("GET /api/students/{id} - returns 404 when not found")
    void getStudentById_notFound_returns404() throws Exception {
        when(studentService.getStudentById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/students/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/students - valid body returns 201")
    void createStudent_valid_returns201() throws Exception {
        when(studentService.createStudent(any(Student.class))).thenReturn(sampleStudent);
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleStudent)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fullName").value("Priya Sharma"));
    }

    @Test
    @DisplayName("POST /api/students - blank name returns 400")
    void createStudent_blankName_returns400() throws Exception {
        Student invalid = new Student("", "test@test.com", "IT", 1);
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    @DisplayName("POST /api/students - invalid email returns 400")
    void createStudent_invalidEmail_returns400() throws Exception {
        Student invalid = new Student("Valid Name", "not-an-email", "IT", 2);
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("DELETE /api/students/{id} - returns 200 on success")
    void deleteStudent_success_returns200() throws Exception {
        doNothing().when(studentService).deleteStudent(1L);
        mockMvc.perform(delete("/api/students/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Student deleted successfully."));
    }

    @Test
    @DisplayName("GET /api/students/search - returns matching results")
    void searchStudents_returns200() throws Exception {
        when(studentService.searchStudents("Priya")).thenReturn(List.of(sampleStudent));
        mockMvc.perform(get("/api/students/search").param("query", "Priya"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fullName").value("Priya Sharma"));
    }

    @Test
    @DisplayName("GET /api/students/count - returns total count")
    void getCount_returns200() throws Exception {
        when(studentService.getTotalCount()).thenReturn(5L);
        mockMvc.perform(get("/api/students/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(5));
    }
}