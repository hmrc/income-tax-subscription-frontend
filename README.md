[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://travis-ci.org/hmrc/income-tax-subscription-frontend.svg)](https://travis-ci.org/hmrc/income-tax-subscription-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/income-tax-subscription-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/income-tax-subscription-frontend/_latestVersion)

# Income Tax Subscription Frontend

This is a Scala/Play frontend web UI that provides screens for an existing SA Individual to voluntarily subscribe to report their income from one or more of

  - Single Sole-Trader (self-employed) business
  - UK Property income
  - Overseas Property income
  
...on a quarterly basis and move away from the yearly Self-Assessment Tax Return.

Local development requires:

  * [sbt](http://www.scala-sbt.org/)
  * MongoDB available on port 27017
  * HMRC Service manager (if using the provided scripts)
    * [Install Service-Manager](https://github.com/hmrc/service-manager/wiki/Install#install-service-manager)**
  * The services in the ITSA_SUBSC_ALL profile (a subset can be used)

# How to start this service (main section)

See `scripts/start`

The active port is 9561

# How to use

The entry page for this service running locally is 

  http://localhost:9561/report-quarterly/income-and-expenses/sign-up

The entry page for this service on staging (requires HMRC VPN) is 

  https://www.staging.tax.service.gov.uk/report-quarterly/income-and-expenses/sign-up

There are two main flows:

  * Agent sign up
  * Individual sign up

See Route files for more information.

Feature switches can be set to change behaviour.  See:

  http://localhost:9561/report-quarterly/income-and-expenses/sign-up/test-only/feature-switch


# How to test

There are two built in test sets: `test` and `it:test`. See build.sbt for details.

External performance tests are provided in the repo `income-tax-subscription-performance-tests`

# Persistence

Data is stored as key/value in Mongo DB. See json reads/writes implementations (especially tests) for details.

To connect to the mongo db provided by docker (recommended) please use

```
docker exec -it mongo-db mongosh
```

Various commands are available.  Start with `show dbs` to see which databases are populated.

### License.
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html") 
  
