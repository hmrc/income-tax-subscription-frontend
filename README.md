[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://travis-ci.org/hmrc/income-tax-subscription-frontend.svg)](https://travis-ci.org/hmrc/income-tax-subscription-frontend)
[![Download](https://api.bintray.com/packages/hmrc/releases/income-tax-subscription-frontend/images/download.svg)](https://bintray.com/hmrc/releases/income-tax-subscription-frontend/_latestVersion)

# Income Tax Subscription Frontend

This is a Scala/Play frontend web UI that provides screens for an existing SA Individual to voluntarily subscribe to report their income from one or more of

  - Single Sole-Trader (self-employed) business
  - UK Property income
  - Overseas Property income
  
...on a quarterly basis and move away from the yearly Self-Assessment Tax Return.

1. [Quick start](#Quick-start)
    - [Prerequisites](#Prerequisites)
    - [How to start](#How-to-start)
    - [How to use](#How-to-use)
    - [How to test](#How-to-test)
2. [Persistence](#Persistence)

# Quick start

## Prerequisites

* [sbt](http://www.scala-sbt.org/)
* MongoDB (*[See Persistence](#Persistence)*)
* HMRC Service manager (*[Install Service-Manager](https://github.com/hmrc/service-manager/wiki/Install#install-service-manager)*)

## How to start

**Run the service with `ITSA_SUBSC_ALL`:**  
```
./scripts/start
```

**Run the service with mininal downstreams:**  
```
./scripts/start --minimal
```

## How to use

There are two main flows:

* Agent sign up
* Individual sign up

See Route files for more information.

### Local

* Login via: [http://localhost:9949/auth-login-stub/gg-sign-in](http://localhost:9949/auth-login-stub/gg-sign-in)
* Entry page: [http://localhost:9561/report-quarterly/income-and-expenses/sign-up](http://localhost:9561/report-quarterly/income-and-expenses/sign-up)
* Feature switches: [http://localhost:9561/report-quarterly/income-and-expenses/sign-up/test-only/feature-switch](http://localhost:9561/report-quarterly/income-and-expenses/sign-up/test-only/feature-switch)
* Stub users: [http://localhost:9561/report-quarterly/income-and-expenses/sign-up/test-only/stub-user](http://localhost:9561/report-quarterly/income-and-expenses/sign-up/test-only/stub-user)

### Staging

*Requires HMRC VPN*

* Login via: [https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in](https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in)
* Entry page : [https://www.staging.tax.service.gov.uk/report-quarterly/income-and-expenses/sign-up](https://www.staging.tax.service.gov.uk/report-quarterly/income-and-expenses/sign-up)
* Feature switches: [https://www.staging.tax.service.gov.uk/report-quarterly/income-and-expenses/sign-up/test-only/feature-switch](https://www.staging.tax.service.gov.uk/report-quarterly/income-and-expenses/sign-up/test-only/feature-switch)

### Notes on behaviour

In Local and Staging, a stubbing system is used for recording user data.  This prevents the display of the success page at `report-quarterly/income-and-expenses/sign-up/client/confirmation` as the stubs service will not persist, then provide an mtd itsa ref enrolment.

To work around this, visit the `report-quarterly/income-and-expenses/sign-up/test-only/update-enrolments` page, submit, then return to the confirmation page.

## How to test

* Run unit tests: `sbt clean test`
* Run integration tests: `sbt clean it/test`
* Run performance tests: provided in the repo [income-tax-subscription-performance-tests](https://github.com/hmrc/income-tax-subscription-performance-tests)
* Run acceptance tests: provided in the repo [income-tax-subscription-acceptance-tests](https://github.com/hmrc/income-tax-subscription-acceptance-tests)

# Persistence

Data is stored as key/value in Mongo DB. See json reads/writes implementations (especially tests) for details.

To connect to the mongo db provided by docker (recommended) please use

```
docker exec -it mongo-db mongosh
```

Various commands are available.  Start with `show dbs` to see which databases are populated. Eg

 * show dbs
 * use <db>
 * show tables
 * db['<tablename>'].countDocuments()
 * db['<tablename>'].find()
 * exit

### License.
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
