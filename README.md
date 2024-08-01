# SoqlFile input plugin for Embulk

SoqlFile input plugin for Embulk loads records from Salesforce.

## Overview

* **Plugin type**: input
* **Resume supported**: yes
* **Cleanup supported**: no
* **Guess supported**: yes

## Configuration

- **type**: `soql_file` (string, required)
- **username**: login username (string, required)
- **password**: login password (string, required)
- **security_token**: security token (string, required)
- **instance_url**: instance url (string, required)
- **api_version**: api version number (string, default: 46.0)
- **auth_end_point**: url for authentication (string, default: `https://login.salesforce.com/services/Soap/u/`)
- **object**: object name (string, required)
- **include_deleted_or_archived_records**: if true, include deleted or archived records (boolean, default: false)
- If you write SOQL directly,
  - **soql**: SOQL (string)
- If **soql** is not set,
  - **select**: SELECT clauses (string)
  - **where**: WHERE clauses (string)
- **incremental**: if true, enables incremental loading. See next section for details (boolean, default: false)
- **incremental_columns**: column names for incremental loading (array of strings). Columns of integer types, string types, `timestamp` are supported.
- **last_record**: values of the last record for incremental loading (array of objects)

## Example

Using SOQL directly:

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
```

Using select, and where (optional):

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

Using incremental loading:

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
