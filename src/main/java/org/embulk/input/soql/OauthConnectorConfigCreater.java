package org.embulk.input.soql;

import com.sforce.ws.ConnectorConfig;

class OauthConnectorConfigCreater implements ConnectorConfigCreater {
    final PluginTask pluginTask;

    OauthConnectorConfigCreater(final PluginTask pluginTask) {
        this.pluginTask = pluginTask;
    }

    public ConnectorConfig createConnectorConfig() {
        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(pluginTask.getAccessToken().get());
        String restEndpoint = pluginTask.getInstanceUrl().get() + "/services/async/" + pluginTask.getApiVersion();

        config.setRestEndpoint(restEndpoint);
        config.setCompression(true);
        config.setTraceMessage(false);
        return config;
    }
}
