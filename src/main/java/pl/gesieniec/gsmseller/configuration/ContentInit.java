package pl.gesieniec.gsmseller.configuration;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;

@Component
public class ContentInit {

    private final PhoneStockRepository phoneStockRepository;

    public ContentInit(PhoneStockRepository phoneStockRepository) {
        this.phoneStockRepository = phoneStockRepository;
        initData(phoneStockRepository);
    }

    private void initData(PhoneStockRepository phoneStockRepository) {
        phoneStockRepository
            .save(new PhoneStock("SNVF5493", "16", "128", "yellow", "56663563563", "Samsung", "Od Andrzeja",
                new BigDecimal("2400"), new BigDecimal("2600")));

        phoneStockRepository
            .save(new PhoneStock("IPH13P", "6", "128", "blue", "356789104563217", "iPhone 13 Pro", "Od Andrzeja",
                new BigDecimal("3600"), new BigDecimal("3900")));

        phoneStockRepository.save(
            new PhoneStock("SMG991B", "8", "128", "silver", "354890567213654", "Samsung Galaxy S21", "Od Andrzeja",
                new BigDecimal("2200"), new BigDecimal("2500")));

        phoneStockRepository.save(
            new PhoneStock("M2004J19C", "6", "128", "black", "356712908765432", "Xiaomi Redmi Note 12", "Od Andrzeja",
                new BigDecimal("1000"), new BigDecimal("1200")));

        phoneStockRepository
            .save(new PhoneStock("A2407", "6", "256", "graphite", "355678902134567", "iPhone 12 Pro", "Od Andrzeja",
                new BigDecimal("3000"), new BigDecimal("3400")));

        phoneStockRepository
            .save(new PhoneStock("GVU6C", "8", "128", "green", "358901234567890", "Google Pixel 7", "Od Andrzeja",
                new BigDecimal("2400"), new BigDecimal("2700")));

        phoneStockRepository
            .save(new PhoneStock("NE2213", "12", "256", "black", "356112908765431", "OnePlus 10 Pro", "Od Andrzeja",
                new BigDecimal("2600"), new BigDecimal("2900")));

        phoneStockRepository
            .save(new PhoneStock("ELS-NX9", "8", "256", "gold", "353212908765430", "Huawei P50 Pro", "Od Andrzeja",
                new BigDecimal("2100"), new BigDecimal("2400")));

        phoneStockRepository
            .save(new PhoneStock("XQ-CQ54", "8", "128", "black", "355512908765430", "Sony Xperia 5 IV", "Od Andrzeja",
                new BigDecimal("2000"), new BigDecimal("2300")));

        phoneStockRepository
            .save(new PhoneStock("XT2303", "8", "256", "red", "358812908765430", "Motorola Edge 40", "Od Andrzeja",
                new BigDecimal("1700"), new BigDecimal("2000")));

        phoneStockRepository
            .save(new PhoneStock("TA-1393", "6", "128", "white", "351212908765430", "Nokia X30", "Od Andrzeja",
                new BigDecimal("1200"), new BigDecimal("1500")));
    }

}

