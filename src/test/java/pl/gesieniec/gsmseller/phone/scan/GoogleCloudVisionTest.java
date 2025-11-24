package pl.gesieniec.gsmseller.phone.scan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GoogleCloudVisionTest {

    @Test
    void test(){
        GoogleCloudVision googleCloudVision = new GoogleCloudVision();
        PhoneDataOcrParser phoneDataOcrParser = new PhoneDataOcrParser();

        String detect = googleCloudVision.detect();

        PhoneScanDto phoneScanDto = phoneDataOcrParser.parseRawOcrData(detect);

        System.out.println(phoneScanDto);

    }

}