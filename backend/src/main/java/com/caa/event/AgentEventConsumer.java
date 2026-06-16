package com.caa.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for agent-related domain events.
 * Demonstrates listener wiring — extend with real business logic as needed.
 */
@Component
public class AgentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AgentEventConsumer.class);

    /**
     * Consume events from the agent-events topic.
     * Use cases: update caches, trigger Flink jobs, write to Doris analytics.
     */
    @KafkaListener(
            topics = "agent-events",
            groupId = "caa-backend",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAgentEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        String eventType = (String) event.get("eventType");
        String entityId  = (String) event.get("entityId");
        log.info("Received agent event: type={} entityId={} topic={} partition={} offset={}",
                eventType, entityId, topic, partition, offset);

        switch (eventType != null ? eventType : "") {
            case "CREATED"        -> handleAgentCreated(entityId, event);
            case "UPDATED"        -> handleAgentUpdated(entityId, event);
            case "DELETED"        -> handleAgentDeleted(entityId, event);
            case "STATUS_CHANGED" -> handleAgentStatusChanged(entityId, event);
            default               -> log.warn("Unknown agent event type: {}", eventType);
        }
    }

    /**
     * Consume workflow events for metrics pipeline.
     */
    @KafkaListener(
            topics = "workflow-events",
            groupId = "caa-backend",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onWorkflowEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        String eventType = (String) event.get("eventType");
        String entityId  = (String) event.get("entityId");
        log.info("Received workflow event: type={} entityId={} topic={}", eventType, entityId, topic);
        // TODO: forward to Doris analytics pipeline via Flink or direct JDBC
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleAgentCreated(String agentId, Map<String, Object> event) {
        log.debug("Agent created: {}", agentId);
        // e.g. warm up embedding cache, notify downstream services
    }

    private void handleAgentUpdated(String agentId, Map<String, Object> event) {
        log.debug("Agent updated: {}", agentId);
        // e.g. invalidate Redis cache for this agent
    }

    private void handleAgentDeleted(String agentId, Map<String, Object> event) {
        log.debug("Agent deleted: {}", agentId);
        // e.g. remove vectors from Redis vector store
    }

    private void handleAgentStatusChanged(String agentId, Map<String, Object> event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) event.get("payload");
        String newStatus = payload != null ? (String) payload.get("status") : "UNKNOWN";
        log.debug("Agent {} status changed to {}", agentId, newStatus);
    }
}
