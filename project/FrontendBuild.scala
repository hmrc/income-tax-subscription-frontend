
import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object FrontendBuild {

  val appName = "income-tax-subscription-frontend"

  lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  private val bootstrapPlayVersion     = "5.21.0"
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

  val compile: Seq[ModuleID] = Seq(
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

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatest"                %% "scalatest"            % scalaTestVersion     % scope,
        "org.scalatestplus"            %% "mockito-3-12"         % "3.2.10.0"           % scope,
        "org.scalatestplus.play"       %% "scalatestplus-play"   % scalaTestPlusVersion % scope,
        "com.typesafe.play"            %% "play-test"            % PlayVersion.current  % scope,
        "org.mockito"                  % "mockito-core"          % "3.12.4"             % scope,
        "org.jsoup"                    % "jsoup"                 % jsoupVersion         % scope,
        "com.vladsch.flexmark"         % "flexmark-all"          % flexmarkVersion      % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonModuleVersion % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatest"                %% "scalatest"            % scalaTestVersion     % scope,
        "com.typesafe.play"            %% "play-test"            % PlayVersion.current  % scope,
        "org.scalatestplus.play"       %% "scalatestplus-play"   % scalaTestPlusVersion % scope,
        "com.github.fge"               % "json-schema-validator" % "2.2.6"             % scope,
        "org.jsoup"                    % "jsoup"                 % jsoupVersion         % scope,
        "com.github.tomakehurst"       % "wiremock-jre8"         % wiremockVersion      % scope,
        "com.vladsch.flexmark"         % "flexmark-all"          % flexmarkVersion      % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonModuleVersion % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
