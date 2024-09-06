package org.embulk.input.soql;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

class OauthForceConnector implements ForceConnector {
    private final PluginTask pluginTask;
    private PartnerConnection partnerConnection;
    private BulkConnection bulkConnection;

    OauthForceConnector(final PluginTask pluginTask) {
        this.pluginTask = pluginTask;
    }

    @Override
    public BulkConnection getBulkConnection() throws ConnectionException, AsyncApiException {
        if (bulkConnection != null) {
            return bulkConnection;
        }

        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(pluginTask.getAccessToken().get());
        String restEndpoint =
                pluginTask.getInstanceUrl().get() + "/services/async/" + pluginTask.getApiVersion();
        config.setRestEndpoint(restEndpoint);
        config.setCompression(true);
        config.setTraceMessage(false);

        bulkConnection = new BulkConnection(config);

        return bulkConnection;
    }

    @Override
    public PartnerConnection getPartnerConnection() throws ConnectionException {
        if (partnerConnection != null) {
            return partnerConnection;
        }

        ConnectorConfig partnerConfig = new ConnectorConfig();
        partnerConfig.setSessionId(pluginTask.getAccessToken().get());
        partnerConfig.setServiceEndpoint(
                pluginTask.getInstanceUrl().get()
                        + "/services/Soap/u/"
                        + pluginTask.getApiVersion());
        partnerConnection = new PartnerConnection(partnerConfig);

        return partnerConnection;
    }
}
