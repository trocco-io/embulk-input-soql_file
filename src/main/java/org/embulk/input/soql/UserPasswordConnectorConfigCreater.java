package org.embulk.input.soql;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

class UserPasswordConnectorConfigCreater implements ConnectorConfigCreater {
    final PluginTask pluginTask;

    UserPasswordConnectorConfigCreater(final PluginTask pluginTask) {
        this.pluginTask = pluginTask;
    }

    public ConnectorConfig createConnectorConfig() throws ConnectionException {
        ConnectorConfig partnerConfig = new ConnectorConfig();
        partnerConfig.setUsername(pluginTask.getUsername().get());
        partnerConfig.setPassword(pluginTask.getPassword().get() + pluginTask.getSecurityToken().get());
        partnerConfig.setAuthEndpoint(pluginTask.getAuthEndPoint().get() + pluginTask.getApiVersion());
        new PartnerConnection(partnerConfig);

        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(partnerConfig.getSessionId());
        String soapEndpoint = partnerConfig.getServiceEndpoint();
        String restEndpoint = soapEndpoint.substring(0, soapEndpoint.indexOf("Soap/")) + "async/" + pluginTask.getApiVersion();
        config.setRestEndpoint(restEndpoint);
        config.setCompression(true);
        config.setTraceMessage(false);

        return config;
    }
}
