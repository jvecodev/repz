package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import repz.app.controller.FileController;
import repz.app.persistence.entity.User;
import repz.app.service.storage.StorageService;

@RestController
@RequiredArgsConstructor
public class FileControllerImpl implements FileController {

    private final StorageService storageService;

    @Override
    public ResponseEntity<String> upload(MultipartFile file, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String url = storageService.upload(file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    @Override
    public ResponseEntity<String> preview(String fileName) {
        String url = storageService.getPreviewUrl(fileName);
        return ResponseEntity.ok(url);
    }

    @Override
    public ResponseEntity<String> minhaFoto(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String url = storageService.getMyPhotoUrlString(user);
        return url == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(url);
    }
}
