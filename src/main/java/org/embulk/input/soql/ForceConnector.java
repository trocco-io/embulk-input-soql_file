package org.embulk.input.soql;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

interface ForceConnector {
    BulkConnection getBulkConnection() throws ConnectionException, AsyncApiException;

    PartnerConnection getPartnerConnection() throws ConnectionException;
}
