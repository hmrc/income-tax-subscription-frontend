package views.helpers

import forms.validation.ConstraintUtil._
import forms.validation.ErrorMessageFactory
import forms.validation.models.SummaryError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.data.validation.Valid
import play.api.i18n.Messages.Implicits.applicationMessages
import play.twirl.api.Html


class SummaryErrorHelperSpec extends PlaySpec with OneServerPerSuite {

  private def checkboxHelper(form: Form[_])
  = views.html.helpers.summaryErrorHelper(form)(applicationMessages)

  implicit class HtmlFormatUtil(html: Html) {
    def doc: Document = Jsoup.parse(html.body)
  }

  case class TestData(field1: String, field2: String, field3: String)

  val errorMessage = ErrorMessageFactory.error("errKey")
  val summmaryErrorMessage: SummaryError = errorMessage.errors.head.args(1).asInstanceOf[SummaryError]

  val testValidation = (data: String) => {
    data.length > 0 match {
      case true => errorMessage
      case _ => Valid
    }
  }
  val testConstraint = constraint(testValidation)

  val field1Name = "field1"
  val field2Name = "field2"
  val field3Name = "field3"
  val testForm = Form(
    mapping(
      field1Name -> text.verifying(testConstraint),
      field2Name -> text,
      field3Name -> text.verifying(testConstraint)
    )(TestData.apply)(TestData.unapply)
  )

  "checkboxHelper" should {
    "no error" in {

    }
    "errors present" in {

    }
  }
}