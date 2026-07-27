package net.cheltsov.model

/**
 * After getting operations from different services we need to add some other fields which do not depend on AccountType
 */
final case class RichOperation(model: Operation, categoryId: CategoryId) extends AnalyticRichOperation
