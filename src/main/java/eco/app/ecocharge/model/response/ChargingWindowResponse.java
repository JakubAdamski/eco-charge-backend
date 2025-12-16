package eco.app.ecocharge.model.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChargingWindowResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double cleanEnergyPerc;
}
