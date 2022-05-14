package org.embulk.input.soql;

import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

interface ConnectorConfigCreater {
    ConnectorConfig createConnectorConfig() throws ConnectionException;
}
