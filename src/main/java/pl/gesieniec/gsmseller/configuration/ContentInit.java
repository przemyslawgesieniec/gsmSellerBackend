package pl.gesieniec.gsmseller.configuration;

import lombok.AllArgsConstructor;
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

    private void initData(PhoneStockRepository phoneStockRepository){
        phoneStockRepository.save(new PhoneStock("SNVF5493","16","128","black","35725348726354","872648736538"));
        phoneStockRepository.save(new PhoneStock("SNVF5493","8","128","white","786568567675","67886787667"));
        phoneStockRepository.save(new PhoneStock("SNVF5493","16","128","yellow","56663563563","13233244324"));
    }
}
