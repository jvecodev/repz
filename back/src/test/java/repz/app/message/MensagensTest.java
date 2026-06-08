package repz.app.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MensagensTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private Mensagens mensagens;

    @Test
    void getRetornaMensagemTraducida() {
        when(messageSource.getMessage(eq("chave.teste"), any(), any(Locale.class)))
                .thenReturn("Mensagem traduzida");

        String result = mensagens.get("chave.teste");

        assertThat(result).isEqualTo("Mensagem traduzida");
    }

    @Test
    void getRetornaCodigoQuandoMensagemNaoEncontrada() {
        when(messageSource.getMessage(eq("chave.inexistente"), any(), any(Locale.class)))
                .thenThrow(new NoSuchMessageException("chave.inexistente"));

        String result = mensagens.get("chave.inexistente");

        assertThat(result).isEqualTo("chave.inexistente");
    }

    @Test
    void getComArgumentosFormataMensagem() {
        when(messageSource.getMessage(eq("chave.args"), any(), any(Locale.class)))
                .thenReturn("Olá Eduardo");

        String result = mensagens.get("chave.args", "Eduardo");

        assertThat(result).isEqualTo("Olá Eduardo");
    }
}
