package com.news.kimo.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for AI-powered operations.
 * All methods use basic offline logic as placeholders.
 * Comments mark where real AI API calls would be integrated.
 *
 * Each method operates asynchronously via a callback interface.
 */
public class AIHelper {

    private static final int SUMMARY_MAX_LENGTH = 100;
    private static final int TITLE_MAX_LENGTH = 60;
    private static final int MAX_SUGGESTED_TAGS = 10;
    private static final int MAX_RECOMMENDED_POSTS = 20;

    // Common Arabic stop words for filtering
    private static final Set<String> STOP_WORDS_AR = new HashSet<>(Arrays.asList(
            "في", "من", "إلى", "على", "عن", "مع", "هو", "هي", "هم", "نحن",
            "أنا", "أنتم", "هذا", "هذه", "تلك", "التي", "الذي", "الذين",
            "التي", "ذلك", "ذلكم", "ذلكن", "ذلكما", "ذلكما", "هذان",
            "هاتان", "هؤلاء", "أولئك", "كان", "كانت", "يكون", "تكون",
            "قد", "لقد", "لم", "لن", "ما", "لا", "إن", "أن", "لن",
            "حتى", "ثم", "أو", "و", "ف", "ب", "ل", "ك", "بل", "لكن",
            "بعد", "قبل", "بين", "عند", "كل", "بعض", "غير", "أي"
    ));

    // Basic profanity filter (placeholder words)
    private static final Set<String> PROFANITY_SET = new HashSet<>(Arrays.asList(
            "شر", "غبي", "حمار", "تافه"
    ));

    // Content category keywords
    private static final String[][] CATEGORY_KEYWORDS = {
            {"تقنية", "برمجة", "تكنولوجيا", "حاسوب", "هاتف", "تطبيق", "ذكاء اصطناعي", "technology", "tech"},
            {"رياضة", "كرة", "مباراة", "فريق", "بطولة", "لاعب", "goal", "sport"},
            {"سياسة", "حكومة", "برلمان", "رئيس", "قانون", "انتخاب", "politics"},
            {"اقتصاد", "مال", "بورصة", "أسهم", "تجارة", "استثمار", "economy", "finance"},
            {"صحة", "طبي", "علاج", "مرض", "مستشفى", "دواء", "health", "medical"},
            {"تعليم", "مدرسة", "جامعة", "طالب", "امتحان", "دراسة", "education"},
            {"ترفيه", "فيلم", "مسلسل", "موسيقى", "فن", "ممثل", "entertainment"},
            {"سفر", "سياحة", "رحلة", "فندق", "مطار", "travel"},
            {"طعام", "أكل", "مطعم", "وصفة", "طبخ", "food", "recipe"}
    };

    private static final String[] CATEGORY_NAMES = {
            "تقنية", "رياضة", "سياسة", "اقتصاد", "صحة", "تعليم", "ترفيه", "سفر", "طعام"
    };

    private final ExecutorService executorService;

    private static volatile AIHelper instance;

    private AIHelper() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Get the singleton instance.
     */
    public static AIHelper getInstance() {
        if (instance == null) {
            synchronized (AIHelper.class) {
                if (instance == null) {
                    instance = new AIHelper();
                }
            }
        }
        return instance;
    }

    // ============================================================
    // Callback Interfaces
    // ============================================================

    public interface OnSummaryListener {
        void onSummaryGenerated(String summary);
        void onError(String error);
    }

    public interface OnTagsListener {
        void onTagsSuggested(List<String> tags);
        void onError(String error);
    }

    public interface OnTitleListener {
        void onTitleSuggested(String title);
        void onError(String error);
    }

    public interface OnTranslateListener {
        void onTextTranslated(String translatedText);
        void onError(String error);
    }

    public interface OnFilterListener {
        void onContentFiltered(String filteredText);
        void onError(String error);
    }

    public interface OnClassifyListener {
        void onContentClassified(String category);
        void onError(String error);
    }

    public interface OnRecommendListener {
        void onPostsRecommended(List<String> postIds);
        void onError(String error);
    }

    // ============================================================
    // Public Methods
    // ============================================================

    /**
     * Generate a summary of the given text.
     * Currently returns the first 100 characters + '...' as a placeholder.
     *
     * @param text     The text to summarize
     * @param listener Callback with the generated summary
     */
    public void generateSummary(final String text, final OnSummaryListener listener) {
        if (text == null || text.isEmpty()) {
            listener.onError("Text is empty");
            return;
        }

        executorService.execute(() -> {
            try {
                // TODO: Replace with real AI API call (e.g., OpenAI, Firebase ML, etc.)
                // Example: String summary = callAIApi("summarize", text);

                String summary;
                if (text.length() <= SUMMARY_MAX_LENGTH) {
                    summary = text;
                } else {
                    summary = text.substring(0, SUMMARY_MAX_LENGTH).trim();
                    // Try to break at the last space to avoid cutting mid-word
                    int lastSpace = summary.lastIndexOf(' ');
                    if (lastSpace > SUMMARY_MAX_LENGTH * 3 / 4) {
                        summary = summary.substring(0, lastSpace);
                    }
                    summary = summary + "...";
                }

                final String result = summary;
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onSummaryGenerated(result));
            } catch (Exception e) {
                e.printStackTrace();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Suggest hashtags based on the text content.
     * Uses keyword extraction from text as a basic offline approach.
     *
     * @param text     The text to analyze
     * @param listener Callback with suggested hashtags (without #)
     */
    public void suggestTags(final String text, final OnTagsListener listener) {
        if (text == null || text.isEmpty()) {
            listener.onError("Text is empty");
            return;
        }

        executorService.execute(() -> {
            try {
                // TODO: Replace with real AI API call for tag suggestion
                // Example: List<String> tags = callAIApi("suggestTags", text);

                // Basic offline approach: extract significant words and match against categories
                List<String> tags = new ArrayList<>();
                String[] words = text.replaceAll("[^\\p{L}\\s]", "").split("\\s+");
                Set<String> seen = new HashSet<>();

                // Check for category keywords
                for (int i = 0; i < CATEGORY_KEYWORDS.length; i++) {
                    for (String keyword : CATEGORY_KEYWORDS[i]) {
                        if (text.toLowerCase(Locale.getDefault()).contains(keyword.toLowerCase(Locale.getDefault()))) {
                            String tag = CATEGORY_NAMES[i];
                            if (seen.add(tag.toLowerCase(Locale.getDefault()))) {
                                tags.add(tag);
                            }
                            break;
                        }
                    }
                }

                // Extract significant words
                for (String word : words) {
                    word = word.trim();
                    if (word.length() < 3) continue;
                    String lower = word.toLowerCase(Locale.getDefault());
                    if (STOP_WORDS_AR.contains(lower)) continue;
                    if (seen.add(lower) && tags.size() < MAX_SUGGESTED_TAGS) {
                        // Capitalize first letter
                        tags.add(word.substring(0, 1).toUpperCase(Locale.getDefault()) + word.substring(1));
                    }
                }

                final List<String> result = tags;
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onTagsSuggested(result));
            } catch (Exception e) {
                e.printStackTrace();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Suggest a title based on the text content.
     * Currently extracts the first meaningful line or sentence.
     *
     * @param text     The text to generate a title from
     * @param listener Callback with the suggested title
     */
    public void suggestTitle(final String text, final OnTitleListener listener) {
        if (text == null || text.isEmpty()) {
            listener.onError("Text is empty");
            return;
        }

        executorService.execute(() -> {
            try {
                // TODO: Replace with real AI API call for title generation
                // Example: String title = callAIApi("generateTitle", text);

                String title;
                // Try to get the first sentence
                String[] sentences = text.split("[.!?\\n]");
                if (sentences.length > 0 && sentences[0].trim().length() > 0) {
                    title = sentences[0].trim();
                } else {
                    title = text;
                }

                // Truncate to max length
                if (title.length() > TITLE_MAX_LENGTH) {
                    title = title.substring(0, TITLE_MAX_LENGTH);
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > TITLE_MAX_LENGTH * 3 / 4) {
                        title = title.substring(0, lastSpace);
                    }
                    title = title + "...";
                }

                final String result = title;
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onTitleSuggested(result));
            } catch (Exception e) {
                e.printStackTrace();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Translate text to a target language.
     * Currently returns the original text as a placeholder.
     *
     * @param text       The text to translate
     * @param targetLang The target language code (e.g. "ar", "en")
     * @param listener   Callback with the translated text
     */
    public void translateText(final String text, final String targetLang, final OnTranslateListener listener) {
        if (text == null || text.isEmpty()) {
            listener.onError("Text is empty");
            return;
        }

        executorService.execute(() -> {
            try {
                // TODO: Replace with real translation API call
                // Example: String translated = callTranslationApi(text, targetLang);

                // Placeholder: return original text
                final String result = text;
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onTextTranslated(result));
            } catch (Exception e) {
                e.printStackTrace();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Filter/censor inappropriate content from the text.
     * Uses a basic word replacement filter.
     *
     * @param text     The text to filter
     * @param listener Callback with the filtered text
     */
    public void filterContent(final String text, final OnFilterListener listener) {
        if (text == null) {
            listener.onError("Text is null");
            return;
        }

        executorService.execute(() -> {
            try {
                // TODO: Replace with real AI content moderation API
                // Example: String filtered = callModerationApi(text);

                String filtered = text;
                for (String word : PROFANITY_SET) {
                    // Create replacement string of same length with asterisks
                    StringBuilder replacement = new StringBuilder();
                    for (int i = 0; i < word.length(); i++) {
                        replacement.append('*');
                    }
                    // Case-insensitive replacement
                    filtered = filtered.replaceAll(
                            "(?i)\\b" + java.util.regex.Pattern.quote(word) + "\\b",
                            replacement.toString()
                    );
                }

                final String result = filtered;
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onContentFiltered(result));
            } catch (Exception e) {
                e.printStackTrace();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Classify content into a category.
     * Uses keyword matching as a basic offline approach.
     *
     * @param text     The text to classify
     * @param listener Callback with the category string
     */
    public void classifyContent(final String text, final OnClassifyListener listener) {
        if (text == null || text.isEmpty()) {
            listener.onError("Text is empty");
            return;
        }

        executorService.execute(() -> {
            try {
                // TODO: Replace with real AI classification API
                // Example: String category = callAIClassificationApi(text);

                String bestCategory = "عام"; // Default category
                int bestScore = 0;

                String lowerText = text.toLowerCase(Locale.getDefault());
                for (int i = 0; i < CATEGORY_KEYWORDS.length; i++) {
                    int score = 0;
                    for (String keyword : CATEGORY_KEYWORDS[i]) {
                        if (lowerText.contains(keyword.toLowerCase(Locale.getDefault()))) {
                            score++;
                        }
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        bestCategory = CATEGORY_NAMES[i];
                    }
                }

                final String result = bestCategory;
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onContentClassified(result));
            } catch (Exception e) {
                e.printStackTrace();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Recommend posts for a user based on their UID.
     * Currently returns an empty list as a placeholder.
     *
     * @param uid      The user's unique ID
     * @param listener Callback with a list of recommended post IDs
     */
    public void recommendPosts(final String uid, final OnRecommendListener listener) {
        if (uid == null || uid.isEmpty()) {
            listener.onError("User ID is empty");
            return;
        }

        executorService.execute(() -> {
            try {
                // TODO: Replace with real recommendation API or Firebase query
                // Example: List<String> posts = callRecommendationApi(uid);
                // Could query Firebase for trending posts, posts from followed users, etc.

                final List<String> result = new ArrayList<>();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onPostsRecommended(result));
            } catch (Exception e) {
                e.printStackTrace();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Shutdown the executor service. Call when no longer needed.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
