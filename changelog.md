# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

## [1.3.0] - 2025-06-23

## [1.2.0] - 2025-05-29

## [1.1.0] - 2025-05-12

### New Features

- [BL-1365](https://ortussolutions.atlassian.net/browse/BL-1365) Add parse() helper methods directly to runtime which returns parse result
- [BL-1388](https://ortussolutions.atlassian.net/browse/BL-1388) new security configuration item: populateServerSystemScope that can allow or not the population of the server.system scope or not

### Improvements

- [BL-1333](https://ortussolutions.atlassian.net/browse/BL-1333) Create links to BoxLang modules
- [BL-1351](https://ortussolutions.atlassian.net/browse/BL-1351) match getHTTPTimeString() and default time to now
- [BL-1358](https://ortussolutions.atlassian.net/browse/BL-1358) Work harder to return partial AST on invalid parse
- [BL-1363](https://ortussolutions.atlassian.net/browse/BL-1363) Error executing dump template \[/dump/html/BoxClass.bxm]
- [BL-1375](https://ortussolutions.atlassian.net/browse/BL-1375) Compat - Move Legacy Date Format Interception to Module-Specific Interception Point
- [BL-1381](https://ortussolutions.atlassian.net/browse/BL-1381) allow box class to be looped over as collection
- [BL-1382](https://ortussolutions.atlassian.net/browse/BL-1382) Rework event bus interceptors to accelerate during executions
- [BL-1383](https://ortussolutions.atlassian.net/browse/BL-1383) Compat - Allow handling of decimals where timespan is used
- [BL-1387](https://ortussolutions.atlassian.net/browse/BL-1387) Allow Numeric ApplicationTimeout assignment to to be decimal

### Bugs

- [BL-1354](https://ortussolutions.atlassian.net/browse/BL-1354) BoxLang date time not accepted by JDBC as a date object
- [BL-1359](https://ortussolutions.atlassian.net/browse/BL-1359) contracting path doesn't work if casing of mapping doesn't match casing of abs path
- [BL-1366](https://ortussolutions.atlassian.net/browse/BL-1366) sessionInvalidate() "Cannot invoke String.length() because "s" is null"
- [BL-1370](https://ortussolutions.atlassian.net/browse/BL-1370) Some methods not found in java interop
- [BL-1372](https://ortussolutions.atlassian.net/browse/BL-1372) string functions accepting null
- [BL-1374](https://ortussolutions.atlassian.net/browse/BL-1374) onMissingTemplate event mystyped as missingtemplate
- [BL-1377](https://ortussolutions.atlassian.net/browse/BL-1377) fileExists() not working with relative paths
- [BL-1378](https://ortussolutions.atlassian.net/browse/BL-1378) optional capture groups throw NPE in reReplace()
- [BL-1379](https://ortussolutions.atlassian.net/browse/BL-1379) array length incorrect for xml nodes
- [BL-1384](https://ortussolutions.atlassian.net/browse/BL-1384) Numeric Session Timeout Values Should Be Duration of Days  not Seconds

## [1.0.0] - 2025-04-30

[Unreleased]: https://github.com/ortus-boxlang/boxlang-servlet/compare/v1.3.0...HEAD

[1.3.0]: https://github.com/ortus-boxlang/boxlang-servlet/compare/v1.2.0...v1.3.0

[1.2.0]: https://github.com/ortus-boxlang/boxlang-servlet/compare/v1.1.0...v1.2.0

[1.1.0]: https://github.com/ortus-boxlang/boxlang-servlet/compare/v1.0.0...v1.1.0

[1.0.0]: https://github.com/ortus-boxlang/boxlang-servlet/compare/48557184906eda841b837deec8f4182cdde359ad...v1.0.0
