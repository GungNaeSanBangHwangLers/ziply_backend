package ziply.review.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageUploadService {

    private final BlobServiceClient blobServiceClient;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            String originalName = file.getOriginalFilename();
            String extension = originalName.substring(originalName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;

            BlobClient blobClient = containerClient.getBlobClient(fileName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            return blobClient.getBlobUrl();

        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    public List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        return files.stream()
                .map(this::upload) // 기존 upload 메서드 호출
                .filter(url -> url != null)
                .toList();
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            blobClient.deleteIfExists();
        } catch (Exception e) {
            // 삭제 실패가 비즈니스 로직 전체를 멈추지 않도록 로그만 남기거나 예외 처리
            System.err.println("이미지 삭제 중 오류 발생: " + e.getMessage());
        }
    }
}