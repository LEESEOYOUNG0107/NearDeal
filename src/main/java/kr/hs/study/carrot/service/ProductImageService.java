package kr.hs.study.carrot.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.hs.study.carrot.entity.ProductImage;
import kr.hs.study.carrot.repository.ProductImageRepository;
import kr.hs.study.carrot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    public static final int MAX_FILES = 10;
    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public Path root() {
        return Path.of(uploadDir).toAbsolutePath().normalize();
    }

    // 업로드 파일을 검사하고 디스크에 저장한 뒤 저장된 파일명을 돌려준다. 하나라도 문제면 이미 쓴 파일을 지우고 400.
    // 확장자는 클라이언트가 보낸 파일명이 아니라 파일 내용(매직 바이트)으로 정한다 -> 이미지가 아닌 파일은 올릴 수 없다
    public List<String> store(List<MultipartFile> files) {
        List<MultipartFile> given = files == null ? List.of() : files.stream().filter(f -> !f.isEmpty()).toList();
        if (given.size() > MAX_FILES) throw bad("사진은 최대 " + MAX_FILES + "장까지 올릴 수 있어요.");

        List<String> saved = new ArrayList<>();
        try {
            Files.createDirectories(root());
            for (MultipartFile file : given) {
                if (file.getSize() > MAX_BYTES) throw bad("사진은 한 장당 5MB 이하여야 해요.");
                byte[] bytes = file.getBytes();
                String ext = sniffExtension(bytes);
                if (ext == null) throw bad("JPG, PNG, WEBP, GIF 사진만 올릴 수 있어요.");
                String name = UUID.randomUUID() + "." + ext;
                Files.write(root().resolve(name), bytes);
                saved.add(name);
            }
            return saved;
        } catch (IOException e) {
            deleteFiles(saved);
            throw new UncheckedIOException(e);
        } catch (ResponseStatusException e) {
            deleteFiles(saved);
            throw e;
        }
    }

    @Transactional
    public void attach(Long productId, List<String> filenames) {
        var product = productRepository.getReferenceById(productId);
        for (int i = 0; i < filenames.size(); i++) {
            imageRepository.save(new ProductImage(product, filenames.get(i), i));
        }
    }

    @Transactional(readOnly = true)
    public List<String> filenames(Long productId) {
        return imageRepository.findByProductIdOrderBySortOrderAsc(productId).stream().map(ProductImage::getFilename).toList();
    }

    // ponytail: 전체 상품의 대표 사진을 한 번에 조회. 상품이 수만 개로 늘면 화면에 보이는 id만 조회하도록 교체
    @Transactional(readOnly = true)
    public Map<Long, String> covers() {
        Map<Long, String> covers = new HashMap<>();
        imageRepository.findCovers().forEach(row -> covers.put((Long) row[0], (String) row[1]));
        return covers;
    }

    public void deleteFiles(List<String> filenames) {
        for (String name : filenames) {
            try {
                Files.deleteIfExists(root().resolve(name));
            } catch (IOException ignored) {
                // 지우지 못해도 서비스에는 영향이 없다(고아 파일로 남음)
            }
        }
    }

    private static String sniffExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
