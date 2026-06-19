package eco.app.ecocharge.client;

import eco.app.ecocharge.model.external.CarbonIntensityResponse;
import eco.app.ecocharge.model.external.GenerationData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalEnergyClient {

    private final RestClient restClient;

    @Value("${external.carbonintensity.base-url}")
    private String baseUrl;

    public List<GenerationData> getGenerationData(LocalDateTime from, LocalDateTime to) {
        String fromStr = from.format(DateTimeFormatter.ISO_DATE_TIME);
        String toStr = to.format(DateTimeFormatter.ISO_DATE_TIME);

        log.info("Fetching data from external API {} to {} - {}", baseUrl, fromStr, toStr);

        CarbonIntensityResponse response = restClient.get()
                .uri(baseUrl + "/" + fromStr + "/" + toStr)
                .retrieve()
                .body(CarbonIntensityResponse.class);

        return response != null ? response.getData() : Collections.emptyList();
    }
}

