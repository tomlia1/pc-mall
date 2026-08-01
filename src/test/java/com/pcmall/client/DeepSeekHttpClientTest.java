package com.pcmall.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekHttpClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesToolCallFromOpenAiCompatibleResponse() throws Exception {
        String response = """
                {"choices":[{"message":{"role":"assistant","tool_calls":[{"id":"call-1","type":"function","function":{"name":"search_products","arguments":"{\\\"maxBudget\\\":5500}"}}]}}]}
                """;

        DeepSeekCompletion completion = DeepSeekHttpClient.parseCompletion(objectMapper.readTree(response));

        assertTrue(completion.hasToolCall());
        assertEquals("search_products", completion.toolName());
        assertEquals("{\"maxBudget\":5500}", completion.toolArguments());
    }
}
