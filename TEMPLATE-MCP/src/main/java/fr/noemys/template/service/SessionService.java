package fr.noemys.template.service;

import fr.noemys.template.model.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session Service for managing MCP HTTP streaming sessions
 * 
 * @version 1.0.0
 */
@Service
public class SessionService {
    
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    
    @Value("${mcp.session.timeout-minutes}")
    private long sessionTimeoutMinutes;
    
    // In-memory session storage
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    
    /**
     * Create a new session
     */
    public SessionInfo createSession(String clientId, String accessToken) {
        log.info("Creating new session for client: {}", clientId);
        
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        SessionInfo session = SessionInfo.builder()
                .sessionId(sessionId)
                .clientId(clientId)
                .accessToken(accessToken)
                .creationTime(now)
                .lastAccessTime(now)
                .expiresAt(now.plusSeconds(sessionTimeoutMinutes * 60))
                .build();
        
        sessions.put(sessionId, session);
        
        log.info("Session created: {}", sessionId);
        return session;
    }
    
    /**
     * Get session by ID
     */
    public SessionInfo getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * Validate session
     */
    public boolean validateSession(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        
        if (session == null) {
            log.warn("Session not found: {}", sessionId);
            return false;
        }
        
        if (session.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Session expired: {}", sessionId);
            sessions.remove(sessionId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Update last access time
     */
    public void updateLastAccess(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session != null) {
            session.setLastAccessTime(Instant.now());
            session.setExpiresAt(Instant.now().plusSeconds(sessionTimeoutMinutes * 60));
            log.debug("Updated last access for session: {}", sessionId);
        }
    }
    
    /**
     * Delete session
     */
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Session deleted: {}", sessionId);
    }
    
    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().getExpiresAt().isBefore(now)) {
                log.info("Removing expired session: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}

