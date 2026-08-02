package com.foodtraceability.config.wecom;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wecom")
public class WeComProperties {

    private String corpId;

    private AgentInfo agent = new AgentInfo();

    private String token;

    private String encodingAesKey;

    public static class AgentInfo {
        private String agentId;
        private String secret;
        private String callbackUrl;

        public String getAgentId() { return agentId; }
        public void setAgentId(String agentId) { this.agentId = agentId; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    }

    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }

    public AgentInfo getAgent() { return agent; }
    public void setAgent(AgentInfo agent) { this.agent = agent; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEncodingAesKey() { return encodingAesKey; }
    public void setEncodingAesKey(String encodingAesKey) { this.encodingAesKey = encodingAesKey; }
}
