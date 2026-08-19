package com.xiaohezi.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private boolean enabled = true;
    private String apiKey = "";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String model = "gemini-3.5-flash-lite";
    private int intentMaxOutputTokens = 80;
    private int answerMaxOutputTokens = 200;
    private String embeddingModel = "gemini-embedding-2";
    private int embeddingDimensions = 768;
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 45;
    private double inputUsdPerMillionTokens = 0.10;
    private double outputUsdPerMillionTokens = 0.40;

    public boolean isConfigured() { return enabled && apiKey != null && !apiKey.isBlank(); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String value) { apiKey = value; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String value) { baseUrl = value; }
    public String getModel() { return model; }
    public void setModel(String value) { model = value; }
    public int getIntentMaxOutputTokens() { return intentMaxOutputTokens; }
    public void setIntentMaxOutputTokens(int value) { intentMaxOutputTokens = value; }
    public int getAnswerMaxOutputTokens() { return answerMaxOutputTokens; }
    public void setAnswerMaxOutputTokens(int value) { answerMaxOutputTokens = value; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String value) { embeddingModel = value; }
    public int getEmbeddingDimensions() { return embeddingDimensions; }
    public void setEmbeddingDimensions(int value) { embeddingDimensions = value; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int value) { connectTimeoutSeconds = value; }
    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int value) { readTimeoutSeconds = value; }
    public double getInputUsdPerMillionTokens() { return inputUsdPerMillionTokens; }
    public void setInputUsdPerMillionTokens(double value) { inputUsdPerMillionTokens = value; }
    public double getOutputUsdPerMillionTokens() { return outputUsdPerMillionTokens; }
    public void setOutputUsdPerMillionTokens(double value) { outputUsdPerMillionTokens = value; }
}
