package com.pcmall.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcmall.config.DeepSeekProperties;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

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

    @Test
    void createsProxySelectorOnlyWhenHostAndPortAreConfigured() {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setProxyHost("127.0.0.1");
        properties.setProxyPort(7897);

        Proxy proxy = DeepSeekHttpClient.proxySelector(properties).orElseThrow()
                .select(java.net.URI.create("https://api.deepseek.com/chat/completions")).get(0);

        assertEquals(Proxy.Type.HTTP, proxy.type());
        assertEquals(7897, ((InetSocketAddress) proxy.address()).getPort());
        properties.setProxyPort(0);
        assertTrue(DeepSeekHttpClient.proxySelector(properties).isEmpty());
    }

    @Test
    void exposesTheFullTypedProductSearchToolContract() throws Exception {
        DeepSeekProperties properties = new DeepSeekProperties();
        DeepSeekHttpClient client = new DeepSeekHttpClient(properties, objectMapper);
        var method = DeepSeekHttpClient.class.getDeclaredMethod("buildRequest", List.class, boolean.class);
        method.setAccessible(true);

        var request = (com.fasterxml.jackson.databind.node.ObjectNode) method.invoke(client, List.of(objectMapper.createObjectNode()), true);
        var schema = request.path("tools").path(0).path("function").path("parameters").path("properties");

        assertTrue(schema.has("minBudget"));
        assertTrue(schema.has("maxBudget"));
        assertTrue(schema.has("productType"));
        assertTrue(schema.has("category"));
        assertTrue(schema.has("keyword"));
        assertTrue(schema.has("tags"));
        var productTypes = schema.path("productType").path("enum");
        assertTrue(productTypes.toString().contains("\"GPU\""));
        assertTrue(productTypes.toString().contains("\"MINI_PC\""));
    }
}
