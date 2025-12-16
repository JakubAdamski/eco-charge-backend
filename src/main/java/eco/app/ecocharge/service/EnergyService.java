package eco.app.ecocharge.service;


import eco.app.ecocharge.model.external.CarbonIntensityResponse;
import eco.app.ecocharge.model.external.FuelMix;
import eco.app.ecocharge.model.external.GenerationData;
import eco.app.ecocharge.model.response.ChargingWindowResponse;
import eco.app.ecocharge.model.response.DailyEnergyMix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnergyService {

    private final RestClient restClient;
    private static final String API_URL = "https://api.carbonintensity.org.uk/generation";


    private static final Set<String> CLEAN_SOURCES = Set.of("biomass", "nuclear", "hydro", "wind", "solar");


    public List<DailyEnergyMix> getAggregatedEnergyMix() {
        LocalDate today = LocalDate.now();
        LocalDate endRange = today.plusDays(3);

        List<GenerationData> rawData = fetchGenerationData(today.atStartOfDay(), endRange.atStartOfDay());

        Map<LocalDate, List<GenerationData>> groupedByDay = rawData.stream()
                .collect(Collectors.groupingBy(d -> LocalDateTime.parse(d.getFromDate(), DateTimeFormatter.ISO_DATE_TIME).toLocalDate()));

        List<DailyEnergyMix> result = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            LocalDate currentDay = today.plusDays(i);
            List<GenerationData> dayData = groupedByDay.getOrDefault(currentDay, Collections.emptyList());

            if (!dayData.isEmpty()) {
                result.add(calculateDailyAverage(currentDay, dayData));
            }
        }
        return result;
    }

    public ChargingWindowResponse findOptimalChargingWindow(int durationHours) {
        if (durationHours < 1 || durationHours > 6) {
            throw new IllegalArgumentException("Duration must be between 1 and 6 hours.");
        }

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(2);

        List<GenerationData> intervals = fetchGenerationData(start, end);

        int intervalsNeeded = durationHours * 2;

        if (intervals.size() < intervalsNeeded) {
            throw new IllegalStateException("Not enough data to calculate window.");
        }

        double maxCleanEnergyAvg = -1.0;
        GenerationData bestStartInterval = null;

        for (int i = 0; i <= intervals.size() - intervalsNeeded; i++) {
            List<GenerationData> window = intervals.subList(i, i + intervalsNeeded);
            double currentAvg = calculateCleanEnergyAverageForWindow(window);

            if (currentAvg > maxCleanEnergyAvg) {
                maxCleanEnergyAvg = currentAvg;
                bestStartInterval = window.get(0);
            }
        }

        LocalDateTime bestStartTime = LocalDateTime.parse(bestStartInterval.getFromDate(), DateTimeFormatter.ISO_DATE_TIME);

        return ChargingWindowResponse.builder()
                .startTime(bestStartTime)
                .endTime(bestStartTime.plusHours(durationHours))
                .cleanEnergyPerc(Math.round(maxCleanEnergyAvg * 10.0) / 10.0)
                .build();
    }

    private List<GenerationData> fetchGenerationData(LocalDateTime from, LocalDateTime to) {
        String fromStr = from.format(DateTimeFormatter.ISO_DATE_TIME);
        String toStr = to.format(DateTimeFormatter.ISO_DATE_TIME);

        log.info("Fetching data from {} to {}", fromStr, toStr);

        CarbonIntensityResponse response = restClient.get()
                .uri(API_URL + "/" + fromStr + "/" + toStr)
                .retrieve()
                .body(CarbonIntensityResponse.class);

        return response != null ? response.getData() : Collections.emptyList();
    }

    private DailyEnergyMix calculateDailyAverage(LocalDate date, List<GenerationData> intervals) {
        Map<String, Double> sumFuel = new HashMap<>();

        for (GenerationData interval : intervals) {
            for (FuelMix fuel : interval.getGenerationmix()) {
                sumFuel.merge(fuel.getFuel(), fuel.getPerc(), Double::sum);
            }
        }

        int count = intervals.size();
        Map<String, Double> avgFuelMix = new HashMap<>();
        double cleanEnergySum = 0.0;

        for (Map.Entry<String, Double> entry : sumFuel.entrySet()) {
            double avg = entry.getValue() / count;
            avgFuelMix.put(entry.getKey(), Math.round(avg * 10.0) / 10.0);

            if (CLEAN_SOURCES.contains(entry.getKey())) {
                cleanEnergySum += avg;
            }
        }

        return DailyEnergyMix.builder()
                .date(date.toString())
                .fuelMix(avgFuelMix)
                .cleanEnergyPerc(Math.round(cleanEnergySum * 10.0) / 10.0)
                .build();
    }

    private double calculateCleanEnergyAverageForWindow(List<GenerationData> window) {
        double totalCleanPerc = 0.0;
        for (GenerationData interval : window) {
            double intervalCleanPerc = interval.getGenerationmix().stream()
                    .filter(f -> CLEAN_SOURCES.contains(f.getFuel()))
                    .mapToDouble(FuelMix::getPerc)
                    .sum();
            totalCleanPerc += intervalCleanPerc;
        }
        return totalCleanPerc / window.size();
    }
}