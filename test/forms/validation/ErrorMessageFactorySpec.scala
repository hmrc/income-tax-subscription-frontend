package forms.validation

import forms.validation.models.{FieldError, SummaryError, TargetIds}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import org.scalatest.Matchers._

class ErrorMessageFactorySpec extends PlaySpec with OneServerPerSuite {

  "ErrorMessageFactory" should {
    "correctly package the Invalid instance when both the field and summary errors are explicitly defined " in {
      val targetField = "testField1"
      val fieldError = FieldError("errMsgField", Seq("arg1", "arg2"))
      val summaryError = SummaryError("errMsgSummary", Seq("arg1", "arg2"), TargetIds(targetField))

      val errorMessage = "errorType1"
      val actual = ErrorMessageFactory.error(errorMessage, fieldError, summaryError)
      actual.errors.head.message shouldBe errorMessage
      actual.errors.head.args.head shouldBe fieldError
      actual.errors.head.args(1) shouldBe summaryError
    }

    "correctly package the Invalid instance when the shortcut function is used" in {
      val targetField = "testField1"
      val errorMessage = "errorType1"
      val errMsg = "errMsg"
      val errArgs = Seq("arg1", "arg2")
      val actual = ErrorMessageFactory.error(errorMessage, targetField, errMsg, errArgs: _*)
      actual.errors.head.message shouldBe errorMessage
      actual.errors.head.args.head shouldBe FieldError(errMsg, errArgs)
      actual.errors.head.args(1) shouldBe SummaryError(errMsg, errArgs, TargetIds(targetField))
    }
  }

}
