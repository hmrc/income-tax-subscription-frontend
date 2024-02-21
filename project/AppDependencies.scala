
import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  val appName = "income-tax-subscription-frontend"

  private val testScope = "test"
  private val integrationTestScope = "it"

  private val bootstrapPlayVersion     = "8.3.0"
  private val playPartialsVersion      = "9.1.0"
  private val playHmrcFrontendVersion  = "8.5.0"
  private val domainVersion            = "9.0.0"
  private val catsVersion              = "2.10.0"
  private val cryptoJsonVersion        = "7.6.0"

  private val jsoupVersion             = "1.15.3"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc"   %% "play-partials-play-30"      % playPartialsVersion,
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30" % playHmrcFrontendVersion,
    "uk.gov.hmrc"   %% "domain-play-30"             % domainVersion,
    "org.typelevel" %% "cats-core"                  % catsVersion,
    "uk.gov.hmrc"   %% "crypto-json-play-30"        % cryptoJsonVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapPlayVersion % testScope,
    "org.jsoup"                    % "jsoup"                   % jsoupVersion         % testScope
  )

  val integrationTest: Seq[ModuleID] = Seq(

  )
}
