
import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  val appName = "income-tax-subscription-frontend"

  private val testScope = "test"
  private val integrationTestScope = "it"

  private val bootstrapPlayVersion     = "5.24.0"
  private val playPartialsVersion      = "8.3.0-play-28"
  private val playHmrcFrontendVersion  = "3.8.0-play-28"
  private val playLanguageVersion      = "5.2.0-play-28"
  private val httpCachingClientVersion = "9.6.0-play-28"
  private val domainVersion            = "8.0.0-play-28"
  private val catsVersion              = "0.9.0"

  private val scalaTestVersion         = "3.2.11"
  private val scalaTestPlusVersion     = "5.1.0"
  private val wiremockVersion          = "2.32.0"
  private val flexmarkVersion          = "0.62.2"
  private val jacksonModuleVersion     = "2.13.2"
  private val jsoupVersion             = "1.14.3"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc"   %% "play-partials"              % playPartialsVersion,
    "uk.gov.hmrc"   %% "play-frontend-hmrc"         % playHmrcFrontendVersion,
    "uk.gov.hmrc"   %% "play-language"              % playLanguageVersion,
    "uk.gov.hmrc"   %% "http-caching-client"        % httpCachingClientVersion,
    "uk.gov.hmrc"   %% "domain"                     % domainVersion,
    "org.typelevel" %% "cats"                       % catsVersion,
    "uk.gov.hmrc"   %% "logback-json-logger"        % "4.9.0"
  )

  val test = Seq(
    "org.scalatest"                %% "scalatest"            % scalaTestVersion     % testScope,
    "org.scalatestplus"            %% "mockito-3-12"         % "3.2.10.0"           % testScope,
    "org.scalatestplus.play"       %% "scalatestplus-play"   % scalaTestPlusVersion % testScope,
    "com.typesafe.play"            %% "play-test"            % PlayVersion.current  % testScope,
    "org.mockito"                  % "mockito-core"          % "3.12.4"             % testScope,
    "org.jsoup"                    % "jsoup"                 % jsoupVersion         % testScope,
    "com.vladsch.flexmark"         % "flexmark-all"          % flexmarkVersion      % testScope,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonModuleVersion % testScope
  )

  val integrationTest = Seq(
    "org.scalatest"                %% "scalatest"            % scalaTestVersion     % integrationTestScope,
    "com.typesafe.play"            %% "play-test"            % PlayVersion.current  % integrationTestScope,
    "org.scalatestplus.play"       %% "scalatestplus-play"   % scalaTestPlusVersion % integrationTestScope,
    "com.github.fge"               % "json-schema-validator" % "2.2.6"              % integrationTestScope,
    "org.jsoup"                    % "jsoup"                 % jsoupVersion         % integrationTestScope,
    "com.github.tomakehurst"       % "wiremock-jre8"         % wiremockVersion      % integrationTestScope,
    "com.vladsch.flexmark"         % "flexmark-all"          % flexmarkVersion      % integrationTestScope,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonModuleVersion % integrationTestScope
  )
}
