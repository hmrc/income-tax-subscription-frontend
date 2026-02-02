
import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {
  val appName = "income-tax-subscription-frontend"

  private val testScope = "test"
  private val integrationTestScope = "it"

  private val bootstrapPlayVersion = "10.5.0"
  private val playPartialsVersion = "10.2.0"
  private val playHmrcFrontendVersion = "12.27.0"
  private val domainVersion = "13.0.0"
  private val catsVersion = "2.10.0"
  private val cryptoJsonVersion = "8.4.0"

  private val jsoupVersion = "1.21.2"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "play-partials-play-30" % playPartialsVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30" % playHmrcFrontendVersion,
    "uk.gov.hmrc" %% "domain-play-30" % domainVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "uk.gov.hmrc" %% "crypto-json-play-30" % cryptoJsonVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % testScope,
    "org.jsoup" % "jsoup" % jsoupVersion % testScope,
    "uk.gov.hmrc" %% "domain-test-play-30" % domainVersion % testScope
  )

  val integrationTest: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "domain-test-play-30" % domainVersion % integrationTestScope
  )
}
