package repz.app.service;

import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Arquivo;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.ArquivoRepository;
import repz.app.service.storage.StorageServiceImpl;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class StorageServiceUnitTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private ArquivoRepository arquivoRepository;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private StorageServiceImpl storageService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(storageService, "bucket", "repz");
        ReflectionTestUtils.setField(storageService, "urlExpiryHours", 1);
        ReflectionTestUtils.setField(storageService, "externalUrl", "http://localhost:9000");
    }

    @Test
    void uploadSalvaArquivoERetornaUrl() throws Exception {
        User user = user(1L, UserRole.ALUNO);
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(file.getContentType()).thenReturn("image/jpeg");
        when(minioClient.putObject(any())).thenReturn(mock(ObjectWriteResponse.class));
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://minio:9000/repz/users/1/uuid.jpg?token=abc");
        when(arquivoRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(arquivoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String url = storageService.upload(file, user);

        assertThat(url).startsWith("http://localhost:9000/");
        verify(arquivoRepository).save(any(Arquivo.class));
    }

    @Test
    void uploadAtualizaArquivoExistente() throws Exception {
        User user = user(2L, UserRole.ALUNO);
        Arquivo existente = new Arquivo();
        existente.setUser(user);
        existente.setFileName("users/2/old.jpg");
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("new.png");
        when(file.getSize()).thenReturn(2048L);
        when(file.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(file.getContentType()).thenReturn("image/png");
        when(minioClient.putObject(any())).thenReturn(mock(ObjectWriteResponse.class));
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://minio:9000/repz/users/2/new.png?token=xyz");
        when(arquivoRepository.findByUserId(2L)).thenReturn(Optional.of(existente));
        when(arquivoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String url = storageService.upload(file, user);

        assertThat(url).startsWith("http://localhost:9000/");
        assertThat(existente.getFileName()).contains("users/2/");
        verify(arquivoRepository).save(existente);
    }

    @Test
    void getPreviewUrlRetornaUrlComHostExterno() throws Exception {
        when(minioClient.getPresignedObjectUrl(any()))
                .thenReturn("http://minio:9000/repz/users/1/photo.jpg?token=abc");

        String url = storageService.getPreviewUrl("users/1/photo.jpg");

        assertThat(url).startsWith("http://localhost:9000/");
        assertThat(url).doesNotContain("minio:9000");
    }

    @Test
    void uploadLancaExcecaoQuandoMinioFalha() throws Exception {
        User user = user(1L, UserRole.ALUNO);
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(file.getContentType()).thenReturn("image/jpeg");
        when(minioClient.putObject(any())).thenThrow(new RuntimeException("connection refused"));
        when(mensagens.get("arquivo.erro.upload")).thenReturn("Erro ao fazer upload");

        assertThatThrownBy(() -> storageService.upload(file, user))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getPreviewUrlLancaExcecaoQuandoMinioFalha() throws Exception {
        when(minioClient.getPresignedObjectUrl(any())).thenThrow(new RuntimeException("connection refused"));
        when(mensagens.get("arquivo.erro.url")).thenReturn("Erro ao gerar URL");

        assertThatThrownBy(() -> storageService.getPreviewUrl("users/1/photo.jpg"))
                .isInstanceOf(ResponseStatusException.class);
    }


    @Test
    void validateProfilePhotoAceitaJpeg() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> storageService.validateProfilePhoto(file));
    }

    @Test
    void validateProfilePhotoAceitaPng() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(file.getContentType()).thenReturn("image/png");

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> storageService.validateProfilePhoto(file));
    }

    @Test
    void validateProfilePhotoRejeitaArquivoNulo() {
        when(mensagens.get("foto.arquivo.obrigatorio")).thenReturn("Foto obrigatória.");

        assertThatThrownBy(() -> storageService.validateProfilePhoto(null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validateProfilePhotoRejeitaArquivoVazio() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        when(mensagens.get("foto.arquivo.obrigatorio")).thenReturn("Foto obrigatória.");

        assertThatThrownBy(() -> storageService.validateProfilePhoto(file))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validateProfilePhotoRejeitaTamanhoExcedido() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6L * 1024 * 1024); // 6 MB
        when(mensagens.get("foto.tamanho.maximo", "5")).thenReturn("Arquivo muito grande.");

        assertThatThrownBy(() -> storageService.validateProfilePhoto(file))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validateProfilePhotoRejeitaFormatoInvalido() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/gif");
        when(mensagens.get("foto.formato.invalido")).thenReturn("Formato inválido.");

        assertThatThrownBy(() -> storageService.validateProfilePhoto(file))
                .isInstanceOf(ResponseStatusException.class);
    }
}
