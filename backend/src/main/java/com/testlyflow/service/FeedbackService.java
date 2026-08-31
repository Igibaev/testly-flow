package com.testlyflow.service;

import com.testlyflow.config.TestlyProperties;
import com.testlyflow.dto.FocusAreaDto;
import com.testlyflow.dto.PrepLinkDto;
import com.testlyflow.entity.AttemptAnswer;
import com.testlyflow.repository.PrepLinkRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FeedbackService {

    private final TestlyProperties properties;
    private final PrepLinkRepository prepLinkRepository;
    private Map<String, TierCopy> copyByTier;

    public FeedbackService(TestlyProperties properties, PrepLinkRepository prepLinkRepository) {
        this.properties = properties;
        this.prepLinkRepository = prepLinkRepository;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    void loadMessages() {
        Yaml yaml = new Yaml();
        try (InputStream in = new ClassPathResource("feedback-messages.yml").getInputStream()) {
            Map<String, Object> root = yaml.load(in);
            Map<String, TierCopy> loaded = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : root.entrySet()) {
                Map<String, Object> tierNode = (Map<String, Object>) entry.getValue();
                List<String> headlines = (List<String>) tierNode.get("headlines");
                List<String> messages = (List<String>) tierNode.get("messages");
                loaded.put(entry.getKey(), new TierCopy(headlines, messages));
            }
            this.copyByTier = loaded;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить feedback-messages.yml", e);
        }
    }

    public Feedback buildFeedback(List<AttemptAnswer> answers, BigDecimal scorePercent) {
        ResultTier tier = resolveTier(scorePercent);
        TierCopy copy = copyByTier.get(tier.yamlKey());
        String headline = pickRandom(copy.headlines());
        String message = pickRandom(copy.messages());
        List<FocusAreaDto> focusAreas = buildFocusAreas(answers);
        return new Feedback(tier, headline, message, focusAreas);
    }

    private ResultTier resolveTier(BigDecimal scorePercent) {
        double score = scorePercent.doubleValue();
        var thresholds = properties.getFeedback().getTierThresholds();
        if (score >= thresholds.getExemplary()) {
            return ResultTier.EXEMPLARY;
        }
        if (score >= thresholds.getStrong()) {
            return ResultTier.STRONG;
        }
        if (score >= thresholds.getSolid()) {
            return ResultTier.SOLID;
        }
        return ResultTier.GROWTH;
    }

    private List<FocusAreaDto> buildFocusAreas(List<AttemptAnswer> answers) {
        Map<Long, CategoryStats> byCategory = new LinkedHashMap<>();
        for (AttemptAnswer answer : answers) {
            var category = answer.getQuestion().getCategory();
            CategoryStats stats = byCategory.computeIfAbsent(category.getId(),
                    id -> new CategoryStats(category.getId(), category.getName()));
            stats.total++;
            if (answer.isCorrect()) {
                stats.correct++;
            }
        }

        if (byCategory.isEmpty()) {
            return List.of();
        }

        double overallAverage = byCategory.values().stream()
                .mapToDouble(CategoryStats::correctRate)
                .average()
                .orElse(0);

        return byCategory.values().stream()
                .filter(s -> s.correctRate() < overallAverage)
                .sorted((a, b) -> Double.compare(a.correctRate(), b.correctRate()))
                .limit(3)
                .map(s -> new FocusAreaDto(
                        s.categoryId,
                        s.categoryName,
                        BigDecimal.valueOf(s.correctRate()).setScale(2, RoundingMode.HALF_UP),
                        s.total - s.correct,
                        prepLinkRepository.findByCategoryIdOrderBySortOrderAsc(s.categoryId).stream()
                                .map(l -> new PrepLinkDto(l.getId(), l.getTitle(), l.getUrl()))
                                .toList()))
                .toList();
    }

    private String pickRandom(List<String> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    public enum ResultTier {
        GROWTH("growth"), SOLID("solid"), STRONG("strong"), EXEMPLARY("exemplary");

        private final String yamlKey;

        ResultTier(String yamlKey) {
            this.yamlKey = yamlKey;
        }

        public String yamlKey() {
            return yamlKey;
        }
    }

    public record Feedback(ResultTier tier, String headline, String message, List<FocusAreaDto> focusAreas) {
    }

    private record TierCopy(List<String> headlines, List<String> messages) {
    }

    private static final class CategoryStats {
        final Long categoryId;
        final String categoryName;
        int total = 0;
        int correct = 0;

        CategoryStats(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        double correctRate() {
            return total == 0 ? 0 : (100.0 * correct / total);
        }
    }
}
