package net.cheltsov.model

trait AnalyticOperation {
  def id: OperationId
  def amount: BigDecimal
  def accountId: AccountId
  def specificFields: AnalyticSpecificFields
}
