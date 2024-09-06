package org.embulk.input.soql;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

class UserPasswordForceConnector implements ForceConnector {
    private final PluginTask pluginTask;
    private PartnerConnection partnerConnection;
    private BulkConnection bulkConnection;

    UserPasswordForceConnector(final PluginTask pluginTask) {
        this.pluginTask = pluginTask;
    }

    @Override
    public BulkConnection getBulkConnection() throws ConnectionException, AsyncApiException {
        if (bulkConnection != null) {
            return bulkConnection;
        }

        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(getPartnerConnection().getConfig().getSessionId());
        String soapEndpoint = getPartnerConnection().getConfig().getServiceEndpoint();
        String restEndpoint =
                soapEndpoint.substring(0, soapEndpoint.indexOf("Soap/"))
                        + "async/"
                        + pluginTask.getApiVersion();
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
        partnerConfig.setUsername(pluginTask.getUsername().get());
        partnerConfig.setPassword(
                pluginTask.getPassword().get() + pluginTask.getSecurityToken().get());
        partnerConfig.setAuthEndpoint(
                pluginTask.getAuthEndPoint().get() + pluginTask.getApiVersion());
        partnerConnection = new PartnerConnection(partnerConfig);

        return partnerConnection;
    }
}
