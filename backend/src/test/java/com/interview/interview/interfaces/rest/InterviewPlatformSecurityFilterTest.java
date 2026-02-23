package com.interview.interview.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewPlatformSecurityFilter Admin 認證")
class InterviewPlatformSecurityFilterTest {

    @Test
    @DisplayName("非 Admin 路徑應直接放行")
    void shouldAllowNonAdminPaths() throws Exception {
        var filter = new InterviewPlatformSecurityFilter("secret-token");
        var request = new MockHttpServletRequest("GET", "/api/v1/interviews");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull(); // chain was invoked
    }

    @Test
    @DisplayName("Admin 路徑帶正確 Token 應放行")
    void shouldAllowAdminPathWithValidToken() throws Exception {
        var filter = new InterviewPlatformSecurityFilter("secret-token");
        var request = new MockHttpServletRequest("POST", "/api/v1/admin/questions");
        request.addHeader("Authorization", "Bearer secret-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(chain.getRequest()).isNotNull(); // chain was invoked
    }

    @Test
    @DisplayName("Admin 路徑無 Token 應回傳 401")
    void shouldReturn401WhenNoToken() throws Exception {
        var filter = new InterviewPlatformSecurityFilter("secret-token");
        var request = new MockHttpServletRequest("POST", "/api/v1/admin/questions");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Admin 路徑帶錯誤 Token 應回傳 401")
    void shouldReturn401WhenWrongToken() throws Exception {
        var filter = new InterviewPlatformSecurityFilter("secret-token");
        var request = new MockHttpServletRequest("DELETE", "/api/v1/admin/questions/some-id");
        request.addHeader("Authorization", "Bearer wrong-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Admin Token 未設定時應放行所有 Admin 路徑")
    void shouldAllowAdminPathsWhenTokenNotConfigured() throws Exception {
        var filter = new InterviewPlatformSecurityFilter("");
        var request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
