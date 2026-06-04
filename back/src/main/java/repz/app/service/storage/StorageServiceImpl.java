package repz.app.service.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Arquivo;
import repz.app.persistence.entity.User;
import repz.app.persistence.repository.ArquivoRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final ArquivoRepository arquivoRepository;
    private final Mensagens mensagens;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.url-expiry-hours:1}")
    private int urlExpiryHours;

    @Value("${minio.external-url}")
    private String externalUrl;

    @Override
    public String upload(MultipartFile file, User user) {
        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = "users/" + user.getId() + "/" + UUID.randomUUID() + extension;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    mensagens.get("arquivo.erro.upload"));
        }

        String url = generatePresignedUrl(objectKey);

        Arquivo arquivo = arquivoRepository.findByUserId(user.getId()).orElse(new Arquivo());
        arquivo.setUser(user);
        arquivo.setFileName(objectKey);
        arquivo.setUrl(url);
        arquivoRepository.save(arquivo);

        return url;
    }

    @Override
    public String getPreviewUrl(String fileName) {
        return generatePresignedUrl(fileName);
    }

    @Override
    public String getMyPhotoUrl(User user) {
        return arquivoRepository.findByUserId(user.getId())
                .map(arquivo -> generatePresignedUrl(arquivo.getFileName()))
                .orElse(null);
    }

    private String generatePresignedUrl(String objectKey) {
        try {
            String presigned = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(urlExpiryHours, TimeUnit.HOURS)
                    .build());
            // Substitui o host interno (ex: minio:9000) pelo endereço acessível externamente
            return presigned.replaceFirst("^https?://[^/]+", externalUrl);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    mensagens.get("arquivo.erro.url"));
        }
    }

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "";
    }
}
