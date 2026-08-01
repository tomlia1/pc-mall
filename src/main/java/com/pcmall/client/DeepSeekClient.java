package com.pcmall.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

public interface DeepSeekClient {
    DeepSeekCompletion complete(List<ObjectNode> messages);
}
