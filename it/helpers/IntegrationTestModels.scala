
package helpers

import core.Constants
import core.Constants.GovernmentGateway._
import core.models.DateModel
import core.models.YesNoModel._
import core.services.CacheConstants
import helpers.IntegrationTestConstants._
import incometax.business.forms.{AccountingMethodForm, MatchTaxYearForm}
import incometax.business.models._
import incometax.business.models.address.{Address, Country, ReturnedAddress}
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models._
import incometax.subscription.models.{Both, EnrolmentKey}
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import incometax.unauthorisedagent.models.{ConfirmAgentModel, StoredSubscription}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator
import usermatching.models.UserDetailsModel

object IntegrationTestModels {

  import CacheConstants._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate = DateModel("06", "04", "2018")
  val testEndDate2018 = DateModel("05", "04", "2019")
  val testEndDate2019 = DateModel("05", "04", "2019")
  val testMatchTaxYearYes: MatchTaxYearModel = MatchTaxYearModel(MatchTaxYearForm.option_yes)
  val testMatchTaxYearNo: MatchTaxYearModel = MatchTaxYearModel(MatchTaxYearForm.option_no)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate2018)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate2018): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testBusinessPhoneNumber = BusinessPhoneNumberModel("01234567890")
  val testAccountingMethod = AccountingMethodModel(AccountingMethodForm.option_cash)
  val testTerms = true

  val testBusinessStartDate = BusinessStartDateModel(testStartDate)

  lazy val testAddress = Address(Some(List("line1", "line2")), Some("zz111zz"), Some(Country("GB", "United Kingdom")))

  lazy val fullKeystoreData: Map[String, JsValue] =
    keystoreData(
      incomeSource = Some(testIncomeSourceBoth),
      rentUkProperty = Some(testRentUkProperty_property_and_other),
      workForYourself = Some(testWorkForYourself_yes),
      otherIncome = Some(testOtherIncomeNo),
      matchTaxYear = Some(testMatchTaxYearNo),
      accountingPeriodDate = Some(testAccountingPeriod),
      businessName = Some(testBusinessName),
      businessPhoneNumber = Some(testBusinessPhoneNumber),
      businessAddress = Some(testAddress),
      accountingMethod = Some(testAccountingMethod),
      terms = Some(testTerms)
    )

  def keystoreData(incomeSource: Option[IncomeSourceModel] = None,
                   rentUkProperty: Option[RentUkPropertyModel] = None,
                   workForYourself: Option[WorkForYourselfModel] = None,
                   otherIncome: Option[OtherIncomeModel] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                   businessAddress: Option[Address] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   terms: Option[Boolean] = None): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      rentUkProperty.map(model => RentUkProperty -> RentUkPropertyModel.format.writes(model)) ++
      workForYourself.map(model => WorkForYourself -> WorkForYourselfModel.format.writes(model)) ++
      otherIncome.map(model => OtherIncome -> OtherIncomeModel.format.writes(model)) ++
      matchTaxYear.map(model => MatchTaxYear -> MatchTaxYearModel.format.writes(model)) ++
      accountingPeriodDate.map(model => AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      businessPhoneNumber.map(model => BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model)) ++
      businessAddress.map(model => BusinessAddress -> Address.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      terms.map(model => Terms -> Json.toJson(model))
  }

  lazy val testIncomeSourceBusiness = IncomeSourceModel(IncomeSourceForm.option_business)

  lazy val testIncomeSourceProperty = IncomeSourceModel(IncomeSourceForm.option_property)

  lazy val testIncomeSourceBoth = IncomeSourceModel(IncomeSourceForm.option_both)

  lazy val testRentUkProperty_no_property = RentUkPropertyModel(NO, None)
  lazy val testRentUkProperty_property_only = RentUkPropertyModel(YES, Some(YES))
  lazy val testRentUkProperty_property_and_other = RentUkPropertyModel(YES, Some(NO))

  lazy val testWorkForYourself_yes = WorkForYourselfModel(YES)
  lazy val testWorkForYourself_no = WorkForYourselfModel(NO)

  lazy val testOtherIncomeNo = OtherIncomeModel(OtherIncomeForm.option_no)

  lazy val testOtherIncomeYes = OtherIncomeModel(OtherIncomeForm.option_yes)

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, testNino, testStartDate)

  lazy val testReturnedAddress = ReturnedAddress("ref", Some("id"), testAddress)

  lazy val testEnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMTDID)

  val testStoredSubscription = StoredSubscription(
    arn = testArn,
    incomeSource = Both,
    otherIncome = false,
    currentPeriodIsPrior = Some(true),
    accountingPeriodStart = Some(testAccountingPeriod.startDate),
    accountingPeriodEnd = Some(testAccountingPeriod.endDate),
    tradingName = Some(testBusinessName.businessName),
    cashOrAccruals = Some(testAccountingMethod.accountingMethod)
  )

  val testConfirmAgentYes = ConfirmAgentModel(ConfirmAgentForm.option_yes)
  val testConfirmAgentNo = ConfirmAgentModel(ConfirmAgentForm.option_no)
}
