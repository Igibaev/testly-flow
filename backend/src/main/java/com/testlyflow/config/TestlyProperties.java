package com.testlyflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "testly")
public class TestlyProperties {

    @NestedConfigurationProperty
    private Sampling sampling = new Sampling();

    @NestedConfigurationProperty
    private Metrics metrics = new Metrics();

    @NestedConfigurationProperty
    private Timing timing = new Timing();

    @NestedConfigurationProperty
    private Feedback feedback = new Feedback();

    public Sampling getSampling() {
        return sampling;
    }

    public void setSampling(Sampling sampling) {
        this.sampling = sampling;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Timing getTiming() {
        return timing;
    }

    public void setTiming(Timing timing) {
        this.timing = timing;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public static class Sampling {
        private int questionsPerCategoryMin = 10;
        private int questionsPerCategoryMax = 15;
        private boolean shuffleQuestions = false;

        public int getQuestionsPerCategoryMin() {
            return questionsPerCategoryMin;
        }

        public void setQuestionsPerCategoryMin(int questionsPerCategoryMin) {
            this.questionsPerCategoryMin = questionsPerCategoryMin;
        }

        public int getQuestionsPerCategoryMax() {
            return questionsPerCategoryMax;
        }

        public void setQuestionsPerCategoryMax(int questionsPerCategoryMax) {
            this.questionsPerCategoryMax = questionsPerCategoryMax;
        }

        public boolean isShuffleQuestions() {
            return shuffleQuestions;
        }

        public void setShuffleQuestions(boolean shuffleQuestions) {
            this.shuffleQuestions = shuffleQuestions;
        }
    }

    public static class Metrics {
        private int minSamples = 5;

        public int getMinSamples() {
            return minSamples;
        }

        public void setMinSamples(int minSamples) {
            this.minSamples = minSamples;
        }
    }

    public static class Timing {
        private long maxTimeSpentMs = 21_600_000L;
        private double suspiciousOverrunRatio = 0.10;

        public long getMaxTimeSpentMs() {
            return maxTimeSpentMs;
        }

        public void setMaxTimeSpentMs(long maxTimeSpentMs) {
            this.maxTimeSpentMs = maxTimeSpentMs;
        }

        public double getSuspiciousOverrunRatio() {
            return suspiciousOverrunRatio;
        }

        public void setSuspiciousOverrunRatio(double suspiciousOverrunRatio) {
            this.suspiciousOverrunRatio = suspiciousOverrunRatio;
        }
    }

    public static class Feedback {
        @NestedConfigurationProperty
        private TierThresholds tierThresholds = new TierThresholds();

        public TierThresholds getTierThresholds() {
            return tierThresholds;
        }

        public void setTierThresholds(TierThresholds tierThresholds) {
            this.tierThresholds = tierThresholds;
        }

        public static class TierThresholds {
            private double solid = 50;
            private double strong = 75;
            private double exemplary = 90;

            public double getSolid() {
                return solid;
            }

            public void setSolid(double solid) {
                this.solid = solid;
            }

            public double getStrong() {
                return strong;
            }

            public void setStrong(double strong) {
                this.strong = strong;
            }

            public double getExemplary() {
                return exemplary;
            }

            public void setExemplary(double exemplary) {
                this.exemplary = exemplary;
            }
        }
    }
}
