package io.github.gothsins.hookforge.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRequestBodyLimitFilterTest {

    @Test
    void rejectsOversizedBodyWhenContentLengthIsUnknown() throws Exception {
        byte[] body = new byte[ApiRequestBodyLimitFilter.MAX_BODY_BYTES + 1];
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new ApiRequestBodyLimitFilter().doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void preservesAcceptedBodyForJsonDeserialization() throws Exception {
        byte[] body = "{\"type\":\"small.event\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events");
        request.setContent(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new ApiRequestBodyLimitFilter().doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest().getInputStream().readAllBytes()).isEqualTo(body);
    }
}
