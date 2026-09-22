package com.ithwx.personalknowledgebase.support.error;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnBadRequest() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(
                new IllegalArgumentException("问题不能为空"),
                request("/api/chat")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("问题不能为空", response.getBody().message());
    }

    @Test
    void shouldReturnNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new NoSuchElementException("资料不存在：99"),
                request("/api/documents/99")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().code());
    }

    @Test
    void shouldHideInternalErrorMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("数据库密码等内部信息"),
                request("/api/documents")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("服务器内部错误", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
