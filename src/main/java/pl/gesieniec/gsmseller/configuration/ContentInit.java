package pl.gesieniec.gsmseller.configuration;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.location.LocationRepository;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserRepository;

@Component
public class ContentInit {

    public ContentInit(PhoneStockRepository phoneStockRepository,
                       UserRepository userRepository,
                       LocationRepository locationRepository) {
        initData(phoneStockRepository, userRepository,locationRepository);
    }

    private void initData(PhoneStockRepository phoneStockRepository,
                          UserRepository userRepository,
                          LocationRepository locationRepository) {

        List<LocationEntity> all = locationRepository.findAll();
        if(all.isEmpty()){
            LocationEntity locationEntity = new LocationEntity("Carrefour Kutno","Kutno","123123123");
            LocationEntity locationEntity1 = new LocationEntity("Galeria Różana","Kutno","123123123");
            LocationEntity locationEntity2 = new LocationEntity("M Park","Piotrków Trybunalski","123123123");
            LocationEntity locationEntity3 = new LocationEntity("Galeria Zgierska","Zgierz","123123123");

            locationRepository.save(locationEntity);
            locationRepository.save(locationEntity1);
            locationRepository.save(locationEntity2);
            locationRepository.save(locationEntity3);
        }
    }

}

