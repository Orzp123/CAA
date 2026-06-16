package com.caa.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for agent-related domain events.
 * Events are published to the "agent-events" topic.
 */
@Component
public class AgentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AgentEventProducer.class);

    public AgentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String AGENT_EVENTS_TOPIC = "agent-events";
    private static final String WORKFLOW_EVENTS_TOPIC = "workflow-events";
    private static final String CHAT_EVENTS_TOPIC = "chat-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an agent lifecycle event (CREATED, UPDATED, DELETED, STATUS_CHANGED).
     */
    public void publishAgentEvent(String eventType, String agentId, Map<String, Object> payload) {
        Map<String, Object> event = buildEvent(eventType, agentId, payload);
        send(AGENT_EVENTS_TOPIC, agentId, event);
    }

    /**
     * Publish a workflow execution event (STARTED, COMPLETED, FAILED).
     */
    public void publishWorkflowEvent(String eventType, String workflowId, Map<String, Object> payload) {
        Map<String, Object> event = buildEvent(eventType, workflowId, payload);
        send(WORKFLOW_EVENTS_TOPIC, workflowId, event);
    }

    /**
     * Publish a chat interaction event for analytics ingestion.
     */
    public void publishChatEvent(String sessionId, String agentId, int inputTokens, int outputTokens) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("agentId", agentId);
        payload.put("inputTokens", inputTokens);
        payload.put("outputTokens", outputTokens);
        Map<String, Object> event = buildEvent("CHAT_COMPLETED", sessionId, payload);
        send(CHAT_EVENTS_TOPIC, sessionId, event);
    }

    private Map<String, Object> buildEvent(String eventType, String entityId, Map<String, Object> payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("entityId", entityId);
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload);
        return event;
    }

    private CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object value) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, value);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic={} key={}: {}", topic, key, ex.getMessage());
            } else {
                log.debug("Published event to topic={} partition={} offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
        return future;
    }
}
