package com.techcourse.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.coyote.http11.request.HttpRequest;
import org.apache.coyote.http11.response.HttpResponse;
import org.apache.coyote.session.Session;
import org.apache.coyote.session.SessionManager;
import org.junit.jupiter.api.Test;

class LoginControllerTest {

    @Test
    void missingAccountIsHandledAsAuthenticationFailure() throws Exception {
        LoginController controller = new LoginController();
        HttpResponse response = HttpResponse.empty();

        controller.service(loginRequest("password=password"), response);

        assertThat(response.header("Location")).contains("/401.html");
    }

    @Test
    void missingPasswordIsHandledAsAuthenticationFailure() throws Exception {
        LoginController controller = new LoginController();
        HttpResponse response = HttpResponse.empty();

        controller.service(loginRequest("account=gugu"), response);

        assertThat(response.header("Location")).contains("/401.html");
    }

    @Test
    void successfulLoginInvalidatesExistingSessionBeforeCreatingANewOne() throws Exception {
        // given
        String existingSessionId = "existing-session";
        SessionManager.add(new Session(existingSessionId));
        LoginController controller = new LoginController();
        HttpRequest request = successfulLoginRequest(existingSessionId);
        HttpResponse response = HttpResponse.empty();

        // when
        controller.service(request, response);

        // then
        assertThat(SessionManager.findSession(existingSessionId)).isNull();
        String newSessionId = response.header("Set-Cookie").orElseThrow()
            .replace("JSESSIONID=", "")
            .replace(";", "");
        assertThat(newSessionId).isNotEqualTo(existingSessionId);
        assertThat(SessionManager.findSession(newSessionId)).isNotNull();

        // cleanup
        SessionManager.remove(newSessionId);
    }

    private HttpRequest successfulLoginRequest(String sessionId) throws Exception {
        String body = "account=gugu&password=password";
        return loginRequest(body, sessionId);
    }

    private HttpRequest loginRequest(String body) throws Exception {
        return loginRequest(body, "");
    }

    private HttpRequest loginRequest(String body, String sessionId) throws Exception {
        String rawRequest = String.format("POST /login HTTP/1.1\r\n"
            + "Host: localhost:8080\r\n"
            + "Cookie: JSESSIONID=%s\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Content-Length: %d\r\n\r\n%s", sessionId, body.length(), body);
        return HttpRequest.from(new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8)));
    }
}
