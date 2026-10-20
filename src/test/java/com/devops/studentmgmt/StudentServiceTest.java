package com.devops.studentmgmt;

import com.devops.studentmgmt.model.Student;
import com.devops.studentmgmt.repository.StudentRepository;
import com.devops.studentmgmt.service.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student sampleStudent;

    @BeforeEach
    void setUp() {
        sampleStudent = new Student("Ramesh Kumar", "ramesh@college.edu", "Computer Science", 3);
        sampleStudent.setId(1L);
    }

    @Test
    @DisplayName("getAllStudents should return list from repository")
    void getAllStudents_returnsAllStudents() {
        when(studentRepository.findAll()).thenReturn(List.of(sampleStudent));
        List<Student> result = studentService.getAllStudents();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Ramesh Kumar");
    }

    @Test
    @DisplayName("getStudentById should return student when found")
    void getStudentById_found() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        Optional<Student> result = studentService.getStudentById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("ramesh@college.edu");
    }

    @Test
    @DisplayName("getStudentById should return empty when not found")
    void getStudentById_notFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<Student> result = studentService.getStudentById(99L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("createStudent should save and return the student")
    void createStudent_success() {
        when(studentRepository.existsByEmail("ramesh@college.edu")).thenReturn(false);
        when(studentRepository.save(sampleStudent)).thenReturn(sampleStudent);
        Student created = studentService.createStudent(sampleStudent);
        assertThat(created.getFullName()).isEqualTo("Ramesh Kumar");
        verify(studentRepository, times(1)).save(sampleStudent);
    }

    @Test
    @DisplayName("createStudent should throw when email already exists")
    void createStudent_duplicateEmail_throwsException() {
        when(studentRepository.existsByEmail("ramesh@college.edu")).thenReturn(true);
        assertThatThrownBy(() -> studentService.createStudent(sampleStudent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("deleteStudent should call deleteById when student exists")
    void deleteStudent_success() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(studentRepository).deleteById(1L);
        studentService.deleteStudent(1L);
        verify(studentRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteStudent should throw when student not found")
    void deleteStudent_notFound_throwsException() {
        when(studentRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> studentService.deleteStudent(99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getTotalCount should return repository count")
    void getTotalCount_returnsCount() {
        when(studentRepository.count()).thenReturn(5L);
        assertThat(studentService.getTotalCount()).isEqualTo(5L);
    }
}