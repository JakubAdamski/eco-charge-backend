package eco.app.ecocharge.model.response;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DailyEnergyMix {
    private String date;
    private double cleanEnergyPerc;
    private Map<String, Double> fuelMix;
}