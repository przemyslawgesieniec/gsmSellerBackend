package pl.gesieniec.gsmseller.phone.scan.parser;

import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

public interface OcrDataParser {

    PhoneScanDto parseRawOcrData(String raw);

}