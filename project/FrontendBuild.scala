import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild {

  val appName = "income-tax-subscription-frontend"

  lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val bootstrapPlayVersion = "5.1.0"
  private val govukTemplateVersion = "5.23.0"
  private val playUiVersion = "8.2.0-play-25"
  private val playPartialsVersion = "6.9.0-play-25"
  private val hmrcTestVersion = "3.9.0-play-25"
  private val scalaTestVersion = "3.0.7"
  private val scalaTestPlusVersion = "2.0.1"
  private val pegdownVersion = "1.6.0"
  private val httpCachingClientVersion = "9.0.0-play-25"
  private val playWhitelistFilterVersion = "2.0.0"
  private val wiremockVersion = "2.22.0"
  private val domainVersion = "5.6.0-play-25"
  private val catsVersion = "0.9.0"
  private val playLanguageVersion = "3.4.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-whitelist-filter" % playWhitelistFilterVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "org.typelevel" %% "cats" % catsVersion,
    "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % "4.6.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % "1.11.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "2.25.1" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "com.github.fge" % "json-schema-validator" % "2.2.6" % scope,
        "org.jsoup" % "jsoup" % "1.11.3" % scope,
        "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
