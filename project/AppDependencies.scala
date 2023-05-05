
import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  val appName = "income-tax-subscription-frontend"

  private val testScope = "test"
  private val integrationTestScope = "it"

  private val bootstrapPlayVersion     = "7.15.0"
  private val playPartialsVersion      = "8.4.0-play-28"
  private val playHmrcFrontendVersion  = "7.7.0-play-28"
  private val domainVersion            = "8.3.0-play-28"
  private val catsVersion              = "2.0.0"

  private val scalaTestVersion         = "3.2.15"
  private val scalaTestPlusVersion     = "5.1.0"
  private val wiremockVersion          = "2.35.0"

  // Last version supporting our Java runtime
  private val flexmarkVersion          = "0.62.2"
  private val jacksonModuleVersion     = "2.14.2"
  private val jsoupVersion             = "1.15.3"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc"   %% "play-partials"              % playPartialsVersion,
    "uk.gov.hmrc"   %% "play-frontend-hmrc"         % playHmrcFrontendVersion,
    "uk.gov.hmrc"   %% "domain"                     % domainVersion,
    "org.typelevel" %% "cats-core"                  % catsVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % bootstrapPlayVersion % testScope,
    "org.scalatest"                %% "scalatest"              % scalaTestVersion     % testScope,
    "org.scalatestplus"            %% "mockito-3-12"           % "3.2.10.0"           % testScope,
    "org.scalatestplus.play"       %% "scalatestplus-play"     % scalaTestPlusVersion % testScope,
    "com.typesafe.play"            %% "play-test"              % PlayVersion.current  % testScope,
    "org.mockito"                  % "mockito-core"            % "3.12.4"             % testScope,
    "org.jsoup"                    % "jsoup"                   % jsoupVersion         % testScope,
    "com.vladsch.flexmark"         % "flexmark-all"            % flexmarkVersion      % testScope,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % jacksonModuleVersion % testScope
  )

  val integrationTest: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % bootstrapPlayVersion % integrationTestScope,
    "org.scalatest"                %% "scalatest"              % scalaTestVersion     % integrationTestScope,
    "com.typesafe.play"            %% "play-test"              % PlayVersion.current  % integrationTestScope,
    "org.scalatestplus.play"       %% "scalatestplus-play"     % scalaTestPlusVersion % integrationTestScope,
    "com.github.fge"               % "json-schema-validator"   % "2.2.14"             % integrationTestScope,
    "org.jsoup"                    % "jsoup"                   % jsoupVersion         % integrationTestScope,
    "com.github.tomakehurst"       % "wiremock-jre8"           % wiremockVersion      % integrationTestScope,
    "com.vladsch.flexmark"         % "flexmark-all"            % flexmarkVersion      % integrationTestScope,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % jacksonModuleVersion % integrationTestScope
  )
}
