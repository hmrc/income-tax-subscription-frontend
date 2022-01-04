
import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

object FrontendBuild {

  val appName = "income-tax-subscription-frontend"

  lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  private val bootstrapPlayVersion = "5.16.0"
  private val playPartialsVersion = "8.1.0-play-28"
  private val playHmrcFrontendVersion = "1.31.0-play-28"
  private val playLanguageVersion = "4.13.0-play-28"
  private val httpCachingClientVersion = "9.3.0-play-28"
  private val domainVersion = "6.2.0-play-28"
  private val catsVersion = "0.9.0"

  private val scalaTestVersion = "3.0.9"
  private val scalaTestPlusVersion = "5.0.0"
  private val pegdownVersion = "1.6.0"
  private val wiremockVersion = "2.27.2"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc" % playHmrcFrontendVersion,
    "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "org.typelevel" %% "cats" % catsVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % "4.9.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % "1.13.1" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "3.7.0" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "com.github.fge" % "json-schema-validator" % "2.2.6" % scope,
        "org.jsoup" % "jsoup" % "1.13.1" % scope,
        "com.github.tomakehurst" % "wiremock-jre8" % wiremockVersion % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
