package org.embulk.input.soql;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigInject;
import org.embulk.config.Task;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.SchemaConfig;

interface PluginTask extends Task
{
    @Config("username")
    String getUsername();

    @Config("password")
    String getPassword();

    @Config("api_version")
    @ConfigDefault("\"46.0\"")
    String getApiVersion();

    @Config("security_token")
    String getSecurityToken();

    @Config("auth_end_point")
    @ConfigDefault("\"https://login.salesforce.com/services/Soap/u/\"")
    String getAuthEndPoint();

    @Config("object")
    String getObject();

    @Config("including_invisible_record")
    @ConfigDefault("false")
    boolean getIncludingInvisibleRecord();

    @Config("soql")
    String getSoql();

    @Config("columns")
    @ConfigDefault("[]")
    SchemaConfig getColumns();

    @ConfigInject
    BufferAllocator getBufferAllocator();
}
