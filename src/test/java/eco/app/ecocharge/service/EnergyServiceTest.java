package eco.app.ecocharge.service;

import eco.app.ecocharge.model.external.CarbonIntensityResponse;
import eco.app.ecocharge.model.external.FuelMix;
import eco.app.ecocharge.model.external.GenerationData;
import eco.app.ecocharge.model.response.ChargingWindowResponse;
import eco.app.ecocharge.model.response.DailyEnergyMix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import static org.mockito.Mockito.lenient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyServiceTest {

    @Mock
    private RestClient restClient;

    @InjectMocks
    private EnergyService energyService;


    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {

        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void shouldCalculateOptimalChargingWindow() {
        CarbonIntensityResponse mockResponse = new CarbonIntensityResponse();
        mockResponse.setData(createMockData(10));
        setHighCleanEnergy(mockResponse.getData().get(3));
        setHighCleanEnergy(mockResponse.getData().get(4));

        when(responseSpec.body(CarbonIntensityResponse.class)).thenReturn(mockResponse);

        ChargingWindowResponse result = energyService.findOptimalChargingWindow(1);

        assertNotNull(result);
        assertEquals(100.0, result.getCleanEnergyPerc());
    }

    @Test
    void shouldThrowExceptionForInvalidDuration() {

        assertThrows(IllegalArgumentException.class, () -> {
            energyService.findOptimalChargingWindow(7); // Max to 6h
        });
    }

    @Test
    void shouldAggregateDailyMix() {
        CarbonIntensityResponse mockResponse = new CarbonIntensityResponse();
        mockResponse.setData(createMockData(150));

        when(responseSpec.body(CarbonIntensityResponse.class)).thenReturn(mockResponse);

        List<DailyEnergyMix> result = energyService.getAggregatedEnergyMix();

        assertFalse(result.isEmpty());
        assertEquals(3, result.size());
        assertNotNull(result.get(0).getFuelMix());
    }


    private List<GenerationData> createMockData(int count) {
        List<GenerationData> list = new ArrayList<>();
        LocalDateTime start = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            GenerationData data = new GenerationData();
            data.setFromDate(start.plusMinutes(30L * i).toString());
            data.setToDate(start.plusMinutes(30L * (i + 1)).toString());

            List<FuelMix> mix = new ArrayList<>();
            mix.add(createFuel("coal", 50.0));
            mix.add(createFuel("gas", 50.0));
            data.setGenerationmix(mix);

            list.add(data);
        }
        return list;
    }

    private void setHighCleanEnergy(GenerationData data) {
        List<FuelMix> mix = new ArrayList<>();
        mix.add(createFuel("solar", 100.0));
        data.setGenerationmix(mix);
    }

    private FuelMix createFuel(String name, double perc) {
        FuelMix f = new FuelMix();
        f.setFuel(name);
        f.setPerc(perc);
        return f;
    }
}