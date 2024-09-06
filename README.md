# SoqlFile input plugin for Embulk

This input plugin for Embulk loads records from Salesforce using the Bulk API.

## Overview

* **Plugin type**: input
* **Resume supported**: yes
* **Cleanup supported**: no
* **Guess supported**: yes

## Configuration Options

- **type**: `soql_file` (string, required)
- **username**: Login username for Salesforce (string, required)
- **password**: Login password for Salesforce (string, required)
- **security_token**: Salseforce security token (string, required)
- **instance_url**: The instance URL for Salesforce (string, required)
- **api_version**: Salesforce API version to use (string, default: 46.0)
- **auth_end_point**: The authentication endpoint URL (string, default: `https://login.salesforce.com/services/Soap/u/`)
- **object**: The name of the Salesforce object to query (string, required)
- **include_deleted_or_archived_records**: If set true, includes deleted or archived records (boolean, default: false)
- If you write SOQL directly,
  - **soql**: The SOQL query to execute (string)
- If **soql** is not set,
  - **select**: SELECT clauses of the SOQL query (string, default: generates a list of all fields supported by the Bulk API)
  - **where**: WHERE clauses of the SOQL query (string)
- **incremental**: Enables incremental loading if set true (boolean, default: false). See the "Incremental Loading" section below for details.
- **incremental_columns**: Specifies the columns to use for incremental loading (array of strings). Supported column types are integers, strings, and timestamps.
- **last_record**: The values of the last record for incremental loading (array of objects). These values will be used to filter new records since the last run.

## Example Configurations

This plugin downloads CSV with Salesfroce Bulk API, so you need to use CSV parser.

### Using SOQL Directly:

```yaml
in:
  type: soql_file
  username: sample@example.com
  password: password
  security_token: ***
  instance_url: https://sample.force.com
  api_version: 41.0
  auth_end_point: https://login.salesforce.com/services/Soap/u/
  object: Account
  soql: "SELECT Id, Name, LastModifiedDate FROM Account"
  parser:
    type: csv
    skip_header_lines: 1
    allow_extra_columns: true
    newline: LF
    columns:
    - {name: Id, type: string}
    - {name: Name, type: string}
```

### Using SELECT and WHERE Clauses:

```yaml
in:
  type: soql_file
  username: sample@example.com
  password: password
  security_token: ***
  instance_url: https://sample.force.com
  api_version: 41.0
  auth_end_point: https://login.salesforce.com/services/Soap/u/
  object: Account
  select: "Id, Name, LastModifiedDate"
  where: "Name != 'John Doe'"
```

### Using Incremental Loading:

```yaml
in:
  type: soql_file
  username: sample@example.com
  password: password
  security_token: ***
  instance_url: https://sample.force.com
  api_version: 41.0
  auth_end_point: https://login.salesforce.com/services/Soap/u/
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
