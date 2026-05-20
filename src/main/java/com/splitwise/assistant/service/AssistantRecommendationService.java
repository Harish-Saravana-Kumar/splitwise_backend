package com.splitwise.assistant.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AssistantRecommendationService {

    public List<String> getRecommendations(BigDecimal totalSpent, String topCategory, List<String> insights) {
        List<String> recommendations = new ArrayList<>();

        if (totalSpent != null && totalSpent.compareTo(BigDecimal.valueOf(0)) > 0) {
            recommendations.add("Set a soft cap for next month based on your last total: " + totalSpent + ".");
        }

        if (topCategory != null && !topCategory.isBlank()) {
            recommendations.add("Review the top group/category '" + topCategory + "' for recurring items to trim.");
        }

        boolean spike = insights.stream().anyMatch(text -> text.toLowerCase().contains("spent") && text.toLowerCase().contains("more"));
        if (spike) {
            recommendations.add("Consider a mid-month budget check-in to avoid spikes.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Ask for a detailed breakdown to identify savings opportunities.");
        }

        return recommendations;
    }
}
