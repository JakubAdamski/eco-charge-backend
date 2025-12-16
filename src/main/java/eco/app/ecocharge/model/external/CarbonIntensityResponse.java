package eco.app.ecocharge.model.external;

import lombok.Data;
import java.util.List;

@Data
public class CarbonIntensityResponse {
    private List<GenerationData> data;
}