package org.embulk.input.soql;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigInject;
import org.embulk.config.Task;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.SchemaConfig;

import java.util.Optional;

interface PluginTask extends Task
{
    @Config("auth_method")
    @ConfigDefault("\"user_password\"")
    AuthMethod getAuthMethod();

    @Config("instance_url")
    @ConfigDefault("null")
    Optional<String> getInstanceUrl();

    @Config("access_token")
    @ConfigDefault("null")
    Optional<String> getAccessToken();

    @Config("username")
    @ConfigDefault("null")
    Optional<String> getUsername();

    @Config("password")
    @ConfigDefault("null")
    Optional<String> getPassword();

    @Config("api_version")
    @ConfigDefault("\"46.0\"")
    String getApiVersion();

    @Config("security_token")
    @ConfigDefault("null")
    Optional<String> getSecurityToken();

    @Config("auth_end_point")
    @ConfigDefault("\"https://login.salesforce.com/services/Soap/u/\"")
    Optional<String> getAuthEndPoint();

    @Config("object")
    String getObject();

    @Config("include_deleted_or_archived_records")
    @ConfigDefault("false")
    boolean getIncludeDeletedOrArchivedRecords();

    @Config("soql")
    String getSoql();

    @Config("columns")
    @ConfigDefault("[]")
    SchemaConfig getColumns();

    @ConfigInject
    BufferAllocator getBufferAllocator();
}
