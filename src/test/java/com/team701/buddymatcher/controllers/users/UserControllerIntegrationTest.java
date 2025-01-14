package com.team701.buddymatcher.controllers.users;

import com.team701.buddymatcher.interceptor.UserInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "socketio.host=localhost",
        "socketio.port=8085"
})
@AutoConfigureMockMvc
@Sql(scripts = "/user_data.sql")
@Sql(scripts = "/cleanup_data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    UserController userController;

    @MockBean
    UserInterceptor interceptor;

    @BeforeEach
    void initTest() throws Exception {
        mvc = MockMvcBuilders
                .standaloneSetup(userController)
                .addInterceptors(interceptor).build();
        when(interceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void getSelf() throws Exception {

        mvc.perform(get("/api/users")
                        .sessionAttrs(Collections.singletonMap("UserId", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Pink Elephant"))
                .andExpect(jsonPath("$.email").value("pink.elephant@gmail.com"))
                .andExpect(jsonPath("$.pairingEnabled").value(false))
                .andExpect(jsonPath("$.buddyCount").value(3));
    }


    @Test
    void getExistingUser() throws Exception {

        mvc.perform(get("/api/users/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Pink Elephant"))
                .andExpect(jsonPath("$.email").value("pink.elephant@gmail.com"))
                .andExpect(jsonPath("$.pairingEnabled").value(false))
                .andExpect(jsonPath("$.buddyCount").value(3));
    }

    @Test
    void getNonExistingUser() throws Exception {

        mvc.perform(get("/api/users/{id}", new Random().nextLong())
                .queryParam("pairingEnabled", String.valueOf(true)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePairingEnabledForSelf() throws Exception {

        mvc.perform(put("/api/users")
                        .sessionAttrs(Collections.singletonMap("UserId", 1))
                .queryParam("pairingEnabled", String.valueOf(true)))
                .andExpect(status().isOk());
    }

    @Test
    void getUserBuddy() throws Exception {

        mvc.perform(get("/api/users/buddy")
                        .sessionAttrs(Collections.singletonMap("UserId", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Pink Elephant"))
                .andExpect(jsonPath("$[0].email").value("pink.elephant@gmail.com"))
                .andExpect(jsonPath("$[0].pairingEnabled").value(false))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].name").value("Hiruna Smith"))
                .andExpect(jsonPath("$[1].email").value("hiruna.smith@gmail.com"))
                .andExpect(jsonPath("$[1].pairingEnabled").value(false))
                .andExpect(jsonPath("$[2].id").value(4))
                .andExpect(jsonPath("$[2].name").value("Flynn Smith"))
                .andExpect(jsonPath("$[2].email").value("flynn.smith@gmail.com"))
                .andExpect(jsonPath("$[2].pairingEnabled").value(false));
    }

    @Test
    void getUserBuddiesInCourse() throws Exception {

        mvc.perform(get("/api/users/buddy/course/{course_id}", 1)
                        .sessionAttrs(Collections.singletonMap("UserId", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Green Dinosaur"))
                .andExpect(jsonPath("$[0].email").value("green.dinosaur@gmail.com"))
                .andExpect(jsonPath("$[0].pairingEnabled").value(false))
                .andExpect(jsonPath("$[1].id").value(4))
                .andExpect(jsonPath("$[1].name").value("Flynn Smith"))
                .andExpect(jsonPath("$[1].email").value("flynn.smith@gmail.com"))
                .andExpect(jsonPath("$[1].pairingEnabled").value(false));
    }

    @Test
    void createAndDeleteUserBuddy() throws Exception {

        mvc.perform(post("/api/users/buddy/{id}", 4)
                        .sessionAttrs(Collections.singletonMap("UserId", 3)))
                .andExpect(status().isOk())
                .andDo(print());

        mvc.perform(delete("/api/users/buddy/{id}", 4)
                        .sessionAttrs(Collections.singletonMap("UserId", 3)))
                .andExpect(status().isOk());
    }

    @Test
    void blockUser() throws Exception {
        // First test that the first buddy returned from a GET request for user 2 is user 1
        mvc.perform(get("/api/users/buddy")
                        .sessionAttrs(Collections.singletonMap("UserId", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        // Then test that a POST request for user 2 to block user 1 is OK
        mvc.perform(post("/api/users/buddy/{id}/block", 1)
                        .sessionAttrs(Collections.singletonMap("UserId", 2)))
                .andExpect(status().isOk());

        // Then test that the first buddy returned from a GET request for user 2 is now user 3
        mvc.perform(get("/api/users/buddy")
                        .sessionAttrs(Collections.singletonMap("UserId", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));

        // Then test that a POST request for user 2 to block user 3 is OK
        mvc.perform(post("/api/users/buddy/{id}/block", 3)
                        .sessionAttrs(Collections.singletonMap("UserId", 2)))
                .andExpect(status().isOk());

        // Then test that the first buddy returned from a GET request for user 2 is now user 4
        mvc.perform(get("/api/users/buddy")
                        .sessionAttrs(Collections.singletonMap("UserId", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4));
    }
}
