
package helpers.agent

import java.time.LocalDate

import models._
import models.common.business._
import models.common.{AccountingPeriodModel, _}
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

object IntegrationTestModels {
  val testStartDate: DateModel = DateModel.dateConvert(LocalDate.now)
  val testEndDate: DateModel = helpers.IntegrationTestModels.testEndDate
  val testEndDateNext: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1).plusDays(-1)
  val testEndDatePlus1Y: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1)
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testBusinessTrade: BusinessTradeNameModel = BusinessTradeNameModel("test trade")
  val testBusinessAddress: BusinessAddressModel = BusinessAddressModel(
    "",
    Address(
      lines = Seq(
        "1 long road",
        "lonely town",
        "quiet county"
      ),
      postcode = "ZZ11ZZ"
    )
  )
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
  val testAccountingMethodForeignProperty = OverseasAccountingMethodPropertyModel(Cash)
  val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
  val testInvalidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(364))
  val testPropertyCommencementDate = PropertyCommencementDateModel(testValidStartDate)
  val testForeignPropertyCommencementDate = OverseasPropertyCommencementDateModel(testValidStartDate)
  val testInvalidForeignPropertyCommencementDate = OverseasPropertyCommencementDateModel(testInvalidStartDate)
  val testInvalidCommencementDate = PropertyCommencementDateModel(testInvalidStartDate)
  val testBusinesses: Seq[SelfEmploymentData] = Seq(SelfEmploymentData(
    id = "businessId",
    businessStartDate = Some(BusinessStartDate(DateModel("19", "03", "1999"))),
    businessName = Some(testBusinessName),
    businessTradeName = Some(testBusinessTrade),
    businessAddress = Some(testBusinessAddress)
  ))


  val testBusinessTradeName = BusinessTradeNameModel("test trade name")
  val testBusinessStartDate = BusinessStartDate(DateModel("05", "04", "2018"))
  val testBusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF"))
  val testId = "testId"


  val fullSubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceAll),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      accountingMethodProperty = Some(testAccountingMethodProperty),
      overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
    )

  def subscriptionData(
                        incomeSource: Option[IncomeSourceModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        propertyCommencementDate: Option[PropertyCommencementDateModel] = None,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                        overseasPropertyCommencementDate: Option[OverseasPropertyCommencementDateModel] = None,
                        overseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] = None)
  : Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => SubscriptionDataKeys.IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      selectedTaxYear.map(model => SubscriptionDataKeys.SelectedTaxYear -> AccountingYearModel.format.writes(model)) ++
      businessName.map(model => SubscriptionDataKeys.BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => SubscriptionDataKeys.AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      propertyCommencementDate.map(model => SubscriptionDataKeys.PropertyCommencementDate -> PropertyCommencementDateModel.format.writes(model)) ++
      accountingMethodProperty.map(model => SubscriptionDataKeys.PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)) ++
      overseasPropertyCommencementDate.map(model => SubscriptionDataKeys.OverseasPropertyCommencementDate -> OverseasPropertyCommencementDateModel.format.writes(model)) ++
      overseasPropertyAccountingMethod.map(model => SubscriptionDataKeys.OverseasPropertyAccountingMethod -> OverseasAccountingMethodPropertyModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)

  lazy val testIncomeSourceBoth: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)

  lazy val testIncomeSourceAll: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  lazy val testClientDetails: UserDetailsModel = helpers.IntegrationTestModels.testUserDetails

  lazy val testSummaryDataSelfEmploymentData =
    Seq(SelfEmploymentData
    (
      id = testId,
      businessStartDate = Some(testBusinessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF")))
    )
    )

}
