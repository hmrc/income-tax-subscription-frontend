
import play.sbt.PlayImport._
import sbt._

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

  private val scalaTestVersion = "3.2.10"
  private val scalaTestPlusVersion = "5.1.0"
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
        "org.mockito" % "mockito-core" % "4.1.0" % scope,
        "org.scalatest" %% "scalatest" % "3.2.10" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
        "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope,
        "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapPlayVersion % scope,
        "org.jsoup" % "jsoup" % "1.14.3" % scope,
        "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test: Seq[ModuleID] = Seq(
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.0" % scope,
        "com.github.tomakehurst" % "wiremock-jre8" % "2.32.0" % scope,
        "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope,
        "uk.gov.hmrc" %% "bootstrap-test-play-28" % "5.17.0" % scope,
        "org.scalatest" %% "scalatest" % "3.2.10" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
        "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
