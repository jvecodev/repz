package repz.app.service.storage;

import org.springframework.web.multipart.MultipartFile;
import repz.app.persistence.entity.User;

public interface StorageService {
    String upload(MultipartFile file, User user);
    String getPreviewUrl(String fileName);
    void validateProfilePhoto(MultipartFile file);
}
