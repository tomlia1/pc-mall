package com.pcmall.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcmall.common.BusinessException;
import com.pcmall.config.DeepSeekProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class DeepSeekHttpClient implements DeepSeekClient {
    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekHttpClient(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        HttpClient.Builder builder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        proxySelector(properties).ifPresent(builder::proxy);
        this.httpClient = builder.build();
    }

    static Optional<ProxySelector> proxySelector(DeepSeekProperties properties) {
        if (!StringUtils.hasText(properties.getProxyHost()) || properties.getProxyPort() < 1 || properties.getProxyPort() > 65535) return Optional.empty();
        return Optional.of(ProxySelector.of(new InetSocketAddress(properties.getProxyHost(), properties.getProxyPort())));
    }

    @Override
    public DeepSeekCompletion complete(List<ObjectNode> messages) {
        return complete(messages, true);
    }

    @Override
    public DeepSeekCompletion completePlain(List<ObjectNode> messages) {
        return complete(messages, false);
    }

    private DeepSeekCompletion complete(List<ObjectNode> messages, boolean includeSearchTool) {
        if (!StringUtils.hasText(properties.getApiKey())) throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "DeepSeek 服务尚未配置");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(buildRequest(messages, includeSearchTool))))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new BusinessException(HttpStatus.BAD_GATEWAY, "DeepSeek 服务调用失败");
            return parseCompletion(objectMapper.readTree(response.body()));
        } catch (HttpTimeoutException e) {
            throw new DeepSeekTimeoutException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "DeepSeek 服务调用中断");
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "DeepSeek 服务调用失败");
        }
    }

    static DeepSeekCompletion parseCompletion(JsonNode root) {
        JsonNode message = root.path("choices").path(0).path("message");
        JsonNode toolCall = message.path("tool_calls").path(0);
        if (!toolCall.isMissingNode()) {
            return DeepSeekCompletion.toolCall(toolCall.path("id").asText(), toolCall.path("function").path("name").asText(), toolCall.path("function").path("arguments").asText());
        }
        return DeepSeekCompletion.text(message.path("content").asText());
    }

    private ObjectNode buildRequest(List<ObjectNode> messages, boolean includeSearchTool) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.getModel());
        ArrayNode messageArray = request.putArray("messages");
        messages.forEach(messageArray::add);
        if (!includeSearchTool) return request;
        ArrayNode tools = request.putArray("tools");
        ObjectNode function = tools.addObject().put("type", "function").putObject("function");
        function.put("name", "search_products");
        function.put("description", "Search active PC products from the MySQL product catalog.");
        ObjectNode parameters = function.putObject("parameters");
        parameters.put("type", "object");
        ObjectNode propertiesNode = parameters.putObject("properties");
        propertiesNode.putObject("minBudget").put("type", "number");
        propertiesNode.putObject("maxBudget").put("type", "number");
        ArrayNode productType = propertiesNode.putObject("productType").put("type", "string").putArray("enum");
        List.of("LAPTOP", "MINI_PC", "CPU", "MOTHERBOARD", "GPU", "RAM", "SSD", "PSU", "CASE").forEach(productType::add);
        propertiesNode.putObject("category").put("type", "string");
        propertiesNode.putObject("keyword").put("type", "string");
        ObjectNode tags = propertiesNode.putObject("tags");
        tags.put("type", "array");
        tags.putObject("items").put("type", "string");
        return request;
    }
}
