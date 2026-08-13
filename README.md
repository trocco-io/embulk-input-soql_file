# SoqlFile input plugin for Embulk

This input plugin for Embulk loads records from Salesforce using the Bulk API.

## Overview

* **Plugin type**: input
* **Resume supported**: yes
* **Cleanup supported**: no
* **Guess supported**: yes

## Configuration Options

- **type**: `soql_file` (string, required)
- **auth_method**: Authentication method to use (string, default: `user_password`). Available values: `user_password`, `oauth`.
- Using username and password authentication (`auth_method: user_password`):
  - **username**: Login username for Salesforce (string)
  - **password**: Login password for Salesforce (string)
  - **security_token**: Salesforce security token (string)
  - **auth_end_point**: The authentication endpoint URL (string, default: `https://login.salesforce.com/services/Soap/u/`)
- Using OAuth access token:
  - **access_token**: OAuth access token (string)
  - **instance_url**: The instance URL for Salesforce (string)
- **api_version**: Salesforce API version to use (string, default: 67.0)
- **object**: The name of the Salesforce object to query (string, required)
- **include_deleted_or_archived_records**: If set true, includes deleted or archived records (boolean, default: false)
- If you write SOQL directly,
  - **soql**: The SOQL query to execute (string, required when `incremental` is false)
- If using incremental loading (`incremental: true`),
  - **select**: SELECT clauses of the SOQL query (string, default: generates a list of all fields supported by the Bulk API)
  - **where**: WHERE clauses of the SOQL query (string)
  - **limit**: LIMIT clauses of the SOQL query (integer)
- **incremental**: Enables incremental loading if set true (boolean, default: false). **Note: `soql` and `incremental` cannot be used together.** When using incremental loading, use `select`/`where`/`limit` instead of `soql`. An `ORDER BY` clause is automatically appended based on `incremental_columns`.
- **incremental_columns**: Specifies the columns to use for incremental loading (array of strings, required when `incremental` is true). Supported column types are integers, strings, and timestamps.
- **last_record**: The values of the last record for incremental loading (array of objects). These values will be used to filter new records since the last run.

## Example Configurations

This plugin downloads CSV with Salesforce Bulk API, so you need to use CSV parser.

```yaml
in:
  type: soql_file
  username: sample@example.com
  password: password
  security_token: ***
  object: Account
  parser:
    type: csv
    skip_header_lines: 1
    allow_extra_columns: true
    newline: LF
    columns:
    - {name: Id, type: string}
    - {name: Name, type: string}
```

### Using SOQL Directly:

```yaml
in:
  type: soql_file
  username: sample@example.com
  password: password
  security_token: ***
  object: Account
  soql: "SELECT Id, Name, LastModifiedDate FROM Account"
```

### Using SELECT and WHERE Clauses with Incremental Loading:

```yaml
in:
  type: soql_file
  username: sample@example.com
  password: password
  security_token: ***
  object: Account
  select: "Id, Name, LastModifiedDate"
  where: "Name != 'John Doe'"
  incremental: true
  incremental_columns:
  - Id
```

## Build

```
$ ./gradlew gem
```
