package com.unza.clinic.service;

import com.unza.clinic.model.DrugInteraction;
import com.unza.clinic.repository.DrugInteractionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Checks prescriptions against known drug-allergy records and drug-drug interactions.
 */
@Service
public class AllergySafetyService {

    private final DrugInteractionRepository interactionRepo;

    public AllergySafetyService(DrugInteractionRepository interactionRepo) {
        this.interactionRepo = interactionRepo;
    }

    /**
     * Check a list of drug names against a patient's allergy string.
     * Returns a list of allergy warnings (drug name + matched allergen).
     */
    public List<Map<String, String>> checkAllergies(List<String> drugNames, String patientAllergies) {
        List<Map<String, String>> warnings = new ArrayList<>();
        if (patientAllergies == null || patientAllergies.isBlank()) return warnings;

        String[] allergens = patientAllergies.split("[,;\\n]+");
        for (String drug : drugNames) {
            for (String allergen : allergens) {
                String trimmed = allergen.trim().toLowerCase();
                if (!trimmed.isBlank() && drug.toLowerCase().contains(trimmed)) {
                    Map<String, String> w = new LinkedHashMap<>();
                    w.put("drug", drug);
                    w.put("allergen", allergen.trim());
                    w.put("severity", "CONTRAINDICATED");
                    w.put("message", "Patient has a documented allergy to '" + allergen.trim() + "'. Prescribing '" + drug + "' may be contraindicated.");
                    warnings.add(w);
                }
            }
        }
        return warnings;
    }

    /**
     * Check all pairs of drugs in the list for known interactions.
     * Returns interaction details for any pairs found in the database.
     */
    public List<Map<String, Object>> checkInteractions(List<String> drugNames) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (drugNames == null || drugNames.size() < 2) return results;

        for (int i = 0; i < drugNames.size(); i++) {
            for (int j = i + 1; j < drugNames.size(); j++) {
                List<DrugInteraction> hits = interactionRepo.findByDrugPair(drugNames.get(i), drugNames.get(j));
                for (DrugInteraction di : hits) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("drugA", di.getDrugA());
                    r.put("drugB", di.getDrugB());
                    r.put("severity", di.getSeverity());
                    r.put("description", di.getDescription());
                    r.put("clinicalEffect", di.getClinicalEffect());
                    r.put("management", di.getManagement());
                    results.add(r);
                }
            }
        }
        return results;
    }

    /**
     * Combined safety check: returns allergy warnings + interaction alerts.
     * Frontend should block SEVERE/CONTRAINDICATED and warn for MODERATE.
     */
    public Map<String, Object> fullSafetyCheck(List<String> drugNames, String patientAllergies) {
        List<Map<String, String>> allergyWarnings = checkAllergies(drugNames, patientAllergies);
        List<Map<String, Object>> interactionWarnings = checkInteractions(drugNames);

        boolean hasSevere = allergyWarnings.stream().anyMatch(w -> "CONTRAINDICATED".equals(w.get("severity")))
                || interactionWarnings.stream().anyMatch(w -> "SEVERE".equals(w.get("severity")) || "CONTRAINDICATED".equals(w.get("severity")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("safe", allergyWarnings.isEmpty() && interactionWarnings.isEmpty());
        result.put("hasSevereWarning", hasSevere);
        result.put("allergyWarnings", allergyWarnings);
        result.put("interactionWarnings", interactionWarnings);
        result.put("totalWarnings", allergyWarnings.size() + interactionWarnings.size());
        return result;
    }
}
