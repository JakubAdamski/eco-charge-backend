package eco.app.ecocharge.controller;

import eco.app.ecocharge.model.response.ChargingWindowResponse;
import eco.app.ecocharge.model.response.DailyEnergyMix;
import eco.app.ecocharge.service.EnergyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/energy")
@RequiredArgsConstructor
public class EnergyController {

    private final EnergyService energyService;


    @GetMapping("/mix")
    public ResponseEntity<List<DailyEnergyMix>> getEnergyMix() {
        return ResponseEntity.ok(energyService.getAggregatedEnergyMix());
    }


    @GetMapping("/optimal-charging")
    public ResponseEntity<ChargingWindowResponse> getOptimalCharging(@RequestParam int hours) {
        return ResponseEntity.ok(energyService.findOptimalChargingWindow(hours));
    }
}
