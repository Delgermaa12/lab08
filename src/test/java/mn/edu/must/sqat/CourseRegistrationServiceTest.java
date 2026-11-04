package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // A1: when + verify
    @Test
    void register_activeStudent_returnsOkSavesAndSendsEmail() {
        Student s = new Student("S1", "s1@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S1")).thenReturn(s);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));

        String result = service.register("S1", "CS201");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S1", "CS201");
        verify(emailSender).send(eq("s1@must.edu.mn"), anyString(), contains("CS201"));
    }

    // A2: verify(..., never())
    @Test
    void register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail() {
        when(repository.findStudent("S404")).thenReturn(null);

        String result = service.register("S404", "CS201");

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3: verifyNoInteractions
    @Test
    void register_inactiveStudent_returnsInactiveAndNoEmailInteraction() {
        Student s = new Student("S2", "s2@must.edu.mn", false, List.of());
        when(repository.findStudent("S2")).thenReturn(s);

        String result = service.register("S2", "CS201");

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A4: when + never
    @Test
    void register_missingPrerequisites_returnsPrerequisitesErrorAndDoesNotSave() {
        Student s = new Student("S3", "s3@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S3")).thenReturn(s);
        when(repository.courseExists("CS301")).thenReturn(true);
        when(repository.prerequisites("CS301")).thenReturn(List.of("CS101", "CS102"));

        String result = service.register("S3", "CS301");

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A5: doThrow
    @Test
    void register_emailSenderThrows_stillReturnsOkAndSaves() {
        Student s = new Student("S1", "s1@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S1")).thenReturn(s);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register("S1", "CS201");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S1", "CS201");
    }

    // A6: assertThrows (хоёр аргумент тус бүрийн null)
    @Test
    void register_nullArguments_throwsIllegalArgumentAndTouchesNothing() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, "CS201"));
        assertThrows(IllegalArgumentException.class, () -> service.register("S1", null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7: ArgumentCaptor
    @Test
    void register_success_emailBodyContainsCourseCode() {
        Student s = new Student("S1", "s1@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S1")).thenReturn(s);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));

        service.register("S1", "CS201");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(anyString(), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("CS201"));
    }

    // A8: @ParameterizedTest + @CsvSource (3 мөр = 3 тест)
    @ParameterizedTest(name = "studentExists={0}, active={1}, prereqMissing={2} -> {3}")
    @CsvSource({
            "false, true,  false, ERROR_NO_STUDENT",
            "true,  false, false, ERROR_INACTIVE_STUDENT",
            "true,  true,  true,  ERROR_PREREQUISITES"
    })
    void register_errorScenarios_returnExpectedErrorCode(
            boolean studentExists, boolean active, boolean prereqMissing, String expected) {

        Student student = studentExists
                ? new Student("S1", "s1@must.edu.mn", active, List.of("CS101"))
                : null;
        lenient().when(repository.findStudent("S1")).thenReturn(student);
        lenient().when(repository.courseExists("CS201")).thenReturn(true);
        lenient().when(repository.prerequisites("CS201"))
                .thenReturn(prereqMissing ? List.of("CS101", "CS102") : List.of("CS101"));

        assertEquals(expected, service.register("S1", "CS201"));
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }
}