
package helpers

import core.services.CacheConstants
import forms._
import helpers.IntegrationTestConstants._
import incometax.business.models.address.{Address, Country, ReturnedAddress}
import incometax.business.models.{BusinessNameModel, BusinessPhoneNumberModel, BusinessStartDateModel}
import models._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator
import usermatching.models.UserDetailsModel

object IntegrationTestModels {

  import CacheConstants._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate = DateModel("06", "04", "2017")
  val testEndDate = DateModel("05", "04", "2018")
  val testAccountingPeriodPriorCurrent: AccountingPeriodPriorModel = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
  val testAccountingPeriodPriorNext: AccountingPeriodPriorModel = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
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
      otherIncome = Some(testOtherIncomeNo),
      accountingPeriodPrior = Some(testAccountingPeriodPriorCurrent),
      accountingPeriodDate = Some(testAccountingPeriod),
      businessName = Some(testBusinessName),
      businessPhoneNumber = Some(testBusinessPhoneNumber),
      businessAddress = Some(testAddress),
      accountingMethod = Some(testAccountingMethod),
      terms = Some(testTerms),
      userDetails = Some(testUserDetails)
    )

  def keystoreData(incomeSource: Option[IncomeSourceModel] = None,
                   otherIncome: Option[OtherIncomeModel] = None,
                   accountingPeriodPrior: Option[AccountingPeriodPriorModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                   businessAddress: Option[Address] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   terms: Option[Boolean] = None,
                   userDetails: Option[UserDetailsModel] = None): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      otherIncome.map(model => OtherIncome -> OtherIncomeModel.format.writes(model)) ++
      accountingPeriodPrior.map(model => AccountingPeriodPrior -> AccountingPeriodPriorModel.format.writes(model)) ++
      accountingPeriodDate.map(model => AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      businessPhoneNumber.map(model => BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model)) ++
      businessAddress.map(model => BusinessAddress -> Address.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      terms.map(model => Terms -> Json.toJson(model)) ++
      userDetails.map(model => UserDetails -> Json.toJson(model))
  }

  lazy val testIncomeSourceBusiness = IncomeSourceModel(IncomeSourceForm.option_business)

  lazy val testIncomeSourceProperty = IncomeSourceModel(IncomeSourceForm.option_property)

  lazy val testIncomeSourceBoth = IncomeSourceModel(IncomeSourceForm.option_both)

  lazy val testIsCurrentPeriod = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)

  lazy val testIsNextPeriod = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)

  lazy val testOtherIncomeNo = OtherIncomeModel(OtherIncomeForm.option_no)

  lazy val testOtherIncomeYes = OtherIncomeModel(OtherIncomeForm.option_yes)

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, testNino, testStartDate)

  lazy val testReturnedAddress = ReturnedAddress("ref", Some("id"), testAddress)

}
