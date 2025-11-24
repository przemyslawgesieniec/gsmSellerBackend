package pl.gesieniec.gsmseller.phone.scan;

import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class GoogleCloudVisionTest {

    @SneakyThrows
    @Test
    void test(){
        //given
        GoogleCloudVision googleCloudVision = new GoogleCloudVision();
        PhoneDataOcrParser phoneDataOcrParser = new PhoneDataOcrParser();

        byte[] data = Files.readAllBytes(Paths.get("src/main/resources/static/IMG_1603A6853523-1.jpeg"));

        //when
        String detect = googleCloudVision.detect(data);

        //then
        PhoneScanDto phoneScanDto = phoneDataOcrParser.parseRawOcrData(detect);
        System.out.println(phoneScanDto);
    }

}