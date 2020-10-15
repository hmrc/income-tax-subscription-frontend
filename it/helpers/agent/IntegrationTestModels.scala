
package helpers.agent


import models._
import models.common.{AccountingPeriodModel, _}
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import utilities.SubscriptionDataKeys

object IntegrationTestModels {

  val testStartDate: DateModel = helpers.IntegrationTestModels.testStartDate
  val testEndDate: DateModel = helpers.IntegrationTestModels.testEndDate
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testPropertyAccountingMethod = AccountingMethodPropertyModel(Cash)


  val fullSubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceAll),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      accountingMethodProperty = Some(testPropertyAccountingMethod)
    )

  def subscriptionData(
                        incomeSource: Option[IncomeSourceModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        accountingMethodProperty: Option[AccountingMethodPropertyModel] = None)
  : Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => SubscriptionDataKeys.IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      businessName.map(model => SubscriptionDataKeys.BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => SubscriptionDataKeys.AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      accountingMethodProperty.map(model => SubscriptionDataKeys.PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)) ++
      selectedTaxYear.map(model => SubscriptionDataKeys.SelectedTaxYear -> AccountingYearModel.format.writes(model))


  }

  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)

  lazy val testIncomeSourceBoth: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)

  lazy val testIncomeSourceAll: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  lazy val testClientDetails: UserDetailsModel = helpers.IntegrationTestModels.testUserDetails

}
