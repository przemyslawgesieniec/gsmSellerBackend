package pl.gesieniec.gsmseller.phone.scan;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

@SpringBootTest
@ActiveProfiles("local")
class PhoneScanAiServiceTest {

    @Autowired
    private PhoneScanAiService phoneScanAiService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    @SneakyThrows
    void test() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
        Resource[] resources = resolver.getResources("classpath:static/*");
        System.out.println("Liczba znalezionych plików: " + resources.length);

        List<ByteArrayResource> photos = Arrays.stream(resources).map(this::convert).toList();
        PhoneScanDto phoneScanDto = phoneScanAiService.scrapDataFromImage(photos.get(0));
        System.out.println("phoneScanDto: " + phoneScanDto);
    }


    @SneakyThrows
    private ByteArrayResource convert(Resource resource) {
        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return resource.getFilename();
            }
        };
    }
}