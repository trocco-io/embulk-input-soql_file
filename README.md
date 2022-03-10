# Soql input plugin for Embulk

TODO: Write short description here and embulk-input-soql.gemspec file.

## Overview

* **Plugin type**: input
* **Resume supported**: yes
* **Cleanup supported**: no
* **Guess supported**: yes

WIP

## Configuration

WIP

* **including_invisible_record**: if true, including deleted or archived records (boolean, default: false)

## Example

```yaml
in:
  type: soql_file
  username: sample@example.com
  password: password
  security_token: ***
  client_id: ***
  client_secret: ***
  instance_url: https://sample.force.com
  api_version: 41.0
  soql: SELECT Id, Name, LastModifiedDate FROM Account WHERE LastModifiedDate > :last_date ORDER BY Id
  including_invisible_record: true
  conditions:
    - {key: last_date, value: '2019-08-19T00:41:38Z' }
  columns:
    - {name: Id, type: string, index: 0}
    - {name: Name, type: string, index: 1}
    - {name: LastModifiedDate, type: timestamp, format: '%Y-%m-%dT%H:%M:%S.%L%z', index: 2}
```


## Build

WIP