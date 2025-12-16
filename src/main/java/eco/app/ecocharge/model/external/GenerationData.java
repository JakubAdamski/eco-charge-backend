package eco.app.ecocharge.model.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GenerationData {
    @JsonProperty("from")
    private String fromDate;

    @JsonProperty("to")
    private String toDate;

    private List<FuelMix> generationmix;
}
