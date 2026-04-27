package pl.gesieniec.gsmseller.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path storageLocation;

    public FileStorageService(@Value("${app.upload-dir:/app/uploads}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory: " + uploadDir, e);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String fileName = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence: " + fileName);
            }

            Path targetLocation = this.storageLocation.resolve(fileName);
            
            try (InputStream is = file.getInputStream()) {
                if (isImage(extension)) {
                    byte[] compressed = compressImage(is, extension, 0.7f);
                    try (InputStream cis = new ByteArrayInputStream(compressed)) {
                        Files.copy(cis, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    Files.copy(is, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            log.info("Stored file: {} at {}", fileName, targetLocation);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + fileName, e);
        }
    }

    private boolean isImage(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png");
    }

    public byte[] compressImageIfImage(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        try (InputStream is = file.getInputStream()) {
            if (isImage(extension)) {
                return compressImage(is, extension, 0.7f);
            } else {
                return file.getBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not process file", e);
        }
    }

    public byte[] createThumbnail(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!isImage(extension)) {
            return null;
        }
        try (InputStream is = file.getInputStream()) {
            return createThumbnailFromStream(is);
        } catch (IOException e) {
            log.warn("Could not create thumbnail", e);
            return null;
        }
    }

    public byte[] createThumbnail(byte[] data, String contentType) {
        if (contentType == null || !contentType.startsWith("image/")) {
            return null;
        }
        try (InputStream is = new ByteArrayInputStream(data)) {
            return createThumbnailFromStream(is);
        } catch (IOException e) {
            log.warn("Could not create thumbnail from byte array", e);
            return null;
        }
    }

    private byte[] createThumbnailFromStream(InputStream is) throws IOException {
        BufferedImage originalImage = ImageIO.read(is);
        if (originalImage == null) return null;

        int targetWidth = 300;
        int targetHeight = (int) (originalImage.getHeight() * (targetWidth / (double) originalImage.getWidth()));

        java.awt.Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, java.awt.Color.WHITE, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(outputImage, "jpg", baos);
        return baos.toByteArray();
    }

    private byte[] compressImage(InputStream inputStream, String extension, float quality) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IOException("Could not read image for compression");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        if (image.getType() == BufferedImage.TYPE_INT_ARGB || image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            newImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
            image = newImage;
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        
        return baos.toByteArray();
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.storageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", fileName);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", fileName, e);
        }
    }
}
