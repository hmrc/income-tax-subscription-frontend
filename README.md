[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://travis-ci.org/hmrc/income-tax-subscription-frontend.svg)](https://travis-ci.org/hmrc/income-tax-subscription-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/income-tax-subscription-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/income-tax-subscription-frontend/_latestVersion)

#### Sign Up to Report your Income and Expenses Quarterly (MTD ITSA)
# Income Tax Subscription Frontend

This is a Scala/Play frontend web UI that provides screens for an existing SA Individual to voluntarily subscribe to report their...

  - Single Sole-Trader (self-employed) business income;
  - Property income; or
  - Single Sole-Trader (self-employed) business & property income
  
...on a quarterly bases and move away from the yearly Self-Assessment Tax Return.

### Running the subscription incometax.business.services locally

You will need [sbt](http://www.scala-sbt.org/)

1) **[Install Service-Manager](https://github.com/hmrc/service-manager/wiki/Install#install-service-manager)**


2) **Start the ITSA subscription dependencies:**

   `sm --start ITSA_SUBSC_DEP -f`


3) **Clone the frontend service:**

  - SSH 
  
    `git clone git@github.com:hmrc/income-tax-subscription-frontend.git`
  
  - HTTPS 
  
    `git clone https://github.com/hmrc/income-tax-subscription-frontend.git`
  
  
4) **Start the frontend service:**

   `sbt "run 9561" -Dapplication.router=testOnlyDoNotUseInAppConf.Routes`
   
  
5) **Clone the protected service:**

  - SSH 
  
     `git clone git@github.com:hmrc/income-tax-subscription.git`
  
  - HTTPS 
  
     `git clone https://github.com/hmrc/income-tax-subscription.git`
  
   
6) **Start the protected service:**

   `sbt "run 9560" -Dapplication.router=testOnlyDoNotUseInAppConf.Routes`

7) **Start the stubbed incometax.business.services:**

   `sm --start INCOME_TAX_SUBSCRIPTION_STUBS -f`

8) **Go to the homepage:**

   http://localhost:9561/report-quarterly/income-and-expenses/sign-up

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html") 
