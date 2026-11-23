
package de.notev2.notev2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import de.notev2.notev2.config.JwtUtil;
import de.notev2.notev2.entity.Note;
import de.notev2.notev2.entity.User;
import de.notev2.notev2.repos.NoteRepository;
import de.notev2.notev2.repos.UserRepository;
import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String validToken;

    private User testuser;

    @BeforeEach
    void setUp() {
        testuser = new User();
        testuser.setUsername("deniz");
        testuser.setPassword("123");
        testuser.setAdmin(false);
        userRepository.save(testuser);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(testuser.getUsername())
                .password(testuser.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        validToken = jwtUtil.generateToken(userDetails);
    }

    @Test
    void createNote_shouldReturnCreateNote() throws Exception {

        String request = """
                {
                    "title": "Testnote",
                    "content": "Das ist eine Testnote"
                }
                """;

        mockMvc.perform(post("/notes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isCreated());
    }

    @Test
    void createNote_withoutToken_shouldReturnUnauthorized() throws Exception {

        String request = """
                {
                    "title": "Testnote",
                    "content": "Das ist eine Testnote"
                }
                """;

        mockMvc.perform(post("/notes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void createNote_withInvalidToken_shouldReturnUnauthorized() throws Exception {

        String request = """
                {
                    "title": "Testnote",
                    "content": "Das ist eine Testnote"
                }
                """;

        String invalidToken = validToken + "invalid";

        mockMvc.perform(post("/notes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createNote_withEmptyTitle_shouldReturnBadRequest() throws Exception {

        String request = """
                {
                    "title": "",
                    "content": "Das ist eine Testnote"
                }
                """;

        mockMvc.perform(post("/notes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void getNotes_shouldReturnOk() throws Exception {

        mockMvc.perform(get("/notes")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void getNotes_withoutToken_shouldReturnUnauthorized() throws Exception {

        mockMvc.perform(get("/notes"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getNotes_withInvalidToken_shouldReturnUnauthorized() throws Exception {

        mockMvc.perform(get("/notes")
                .header("Authorization", "Bearer aaa.bbb.ccc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotes_shouldReturnOnlyUserNotes() throws Exception {

        Note note = new Note();
        note.setTitle("Testnote");
        note.setContent("Das ist eine Testnote");
        note.setUser(testuser);
        noteRepository.save(note);

        mockMvc.perform(get("/notes")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Testnote"))
                .andExpect(jsonPath("$[0].content").value("Das ist eine Testnote"));
    }

    @Test
    void getNotes_shouldNotReturnOtherUsersNotes() throws Exception {

        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setPassword("123");
        userRepository.save(otherUser);

        Note note = new Note();
        note.setTitle("Other Note");
        note.setContent("Das ist eine andere Testnote");
        note.setUser(otherUser);
        noteRepository.save(note);

        mockMvc.perform(get("/notes")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Other Note')]").doesNotExist());
    }

    @Test
    void deleteNote_shouldReturnOk() throws Exception {

        Note note = new Note();
        note.setTitle("Testnote");
        note.setContent("Das ist eine Testnote");
        note.setUser(testuser);
        noteRepository.save(note);

        mockMvc.perform(delete("/notes/" + note.getId())
                .with(csrf())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNote_withoutToken_shouldReturnUnauthorized() throws Exception {

        Note note = new Note();
        note.setTitle("Testnote");
        note.setContent("Das ist eine Testnote");
        note.setUser(testuser);
        noteRepository.save(note);

        mockMvc.perform(delete("/notes/" + note.getId())
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteNote_withInvalidToken_shouldReturnUnauthorized() throws Exception {

        Note note = new Note();
        note.setTitle("Testnote");
        note.setContent("Das ist eine Testnote");
        note.setUser(testuser);
        noteRepository.save(note);

        mockMvc.perform(delete("/notes/" + note.getId())
                .with(csrf())
                .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteNote_ofAnotherUser_shouldReturnBadRequest() throws Exception {

        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setPassword("123");
        userRepository.save(otherUser);

        Note note = new Note();
        note.setTitle("Other Note");
        note.setContent("Das ist eine andere Testnote");
        note.setUser(otherUser);
        noteRepository.save(note);

        mockMvc.perform(delete("/notes/" + note.getId())
                .with(csrf())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createNote_withoutCsrfToken_shouldReturnForbidden() throws Exception {

        String request = """
                {
                    "title": "Testnote",
                    "content": "Das ist eine Testnote"
                }
                """;

        mockMvc.perform(post("/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getNotes_withoutCsrfToken_shouldReturnOk() throws Exception {

        mockMvc.perform(get("/notes")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }
}
