import sbt.Keys._
import sbt.Tests.{SubProcess, Group}
import sbt._
import play.routes.compiler.InjectedRoutesGenerator
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name, "-Dlogger.resource=logback-test.xml"))))
    }
}
