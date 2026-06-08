package repz.app.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import repz.app.message.Mensagens;

import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobalExceptionHandlerTest {

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(mensagens.get(any())).thenReturn("mensagem de erro");
    }

    @Test
    void noSuchElementRetorna404() {
        ResponseEntity<ErrorResponse> resp =
                handler.handleNotFound(new NoSuchElementException("not found"), request);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void responseStatusExceptionRetornaStatusCorreto() {
        var ex = new ResponseStatusException(HttpStatus.CONFLICT, "Conflito");
        ResponseEntity<ErrorResponse> resp = handler.handleResponseStatus(ex, request);
        assertThat(resp.getStatusCode().value()).isEqualTo(409);
        assertThat(resp.getBody().getMessage()).isEqualTo("Conflito");
    }

    @Test
    void responseStatusException404() {
        var ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso não encontrado");
        assertThat(handler.handleResponseStatus(ex, request).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void responseStatusExceptionSemReasonUsaPhrase() {
        var ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleResponseStatus(ex, request).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void accessDeniedRetorna403() {
        ResponseEntity<ErrorResponse> resp =
                handler.handleAccessDenied(new AccessDeniedException("sem permissão"), request);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void authenticationExceptionRetorna401() {
        AuthenticationException ex = mock(AuthenticationException.class);
        when(ex.getMessage()).thenReturn("token inválido");
        assertThat(handler.handleAuthentication(ex, request).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void badCredentialsRetorna401() {
        assertThat(handler.handleAuthentication(
                new BadCredentialsException("credenciais"), request).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void dataIntegrityViolationRetorna409() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("uk", new RuntimeException("unique"));
        assertThat(handler.handleDataIntegrity(ex, request).getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void methodNotSupportedRetorna405() {
        assertThat(handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"), request).getStatusCode().value()).isEqualTo(405);
    }

    @Test
    void httpMessageNotReadableRetorna400() {
        var ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("JSON parse error");
        assertThat(handler.handleBadRequest(ex, request).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void illegalArgumentRetorna400() {
        assertThat(handler.handleBadRequest(
                new IllegalArgumentException("inválido"), request).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void constraintViolationRetorna400ComDetalhes() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("campo");
        when(violation.getMessage()).thenReturn("não pode ser nulo");

        ConstraintViolationException ex = new ConstraintViolationException("", Set.of(violation));
        ResponseEntity<ErrorResponse> resp = handler.handleConstraintViolation(ex, request);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(resp.getBody().getErrors()).isNotEmpty();
    }

    @Test
    void runtimeExceptionRetorna400() {
        assertThat(handler.handleRuntime(
                new RuntimeException("erro"), request).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void genericExceptionRetorna500() {
        assertThat(handler.handleInternal(
                new Exception("erro interno"), request).getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void errorResponseContemCamposEsperados() {
        ResponseEntity<ErrorResponse> resp =
                handler.handleNotFound(new NoSuchElementException("x"), request);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getPath()).isEqualTo("/api/test");
        assertThat(body.getTimestamp()).isNotBlank();
    }
}
