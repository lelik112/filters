package net.cheltsov.filters

import zio.prelude.NonEmptySet

import net.cheltsov.model._
import net.cheltsov.utils.tapir._

import enumeratum.{Enum, EnumEntry}
import sttp.tapir.{EndpointInput, query}

sealed trait FilterType extends EnumEntry {
  type Content
  def filter(content: Content): RichOperation => Boolean

  def of(content: Content): Filter[this.type] = Filter(Some(content))
  def empty: Filter[this.type] = Filter.empty
}

sealed trait AccountFilterType extends FilterType {
  def accountFilter(content: Content): Account => Boolean
}

sealed trait AnalyticFilterType extends FilterType {
  def filter(content: Content): AnalyticRichOperation => Boolean
}

sealed trait PartiallyModelFilterType extends FilterType {
  def modelFilter(content: Content): Operation => Boolean
}

sealed trait ModelFilterType extends PartiallyModelFilterType {
  // Do not override except on AnalyticModelFilterType
  override def filter(content: Content): RichOperation => Boolean =
    operation => modelFilter(content)(operation.model)
}

sealed trait PartiallyAnalyticModelFilterType extends AnalyticFilterType with PartiallyModelFilterType {
  def modelFilter(content: Content): AnalyticOperation => Boolean
}

sealed trait AnalyticModelFilterType extends PartiallyAnalyticModelFilterType with ModelFilterType {
  override final def filter(content: Content): AnalyticRichOperation => Boolean =
    operation => modelFilter(content)(operation.model)
}

sealed trait ApiFilterType extends FilterType {
  def input: EndpointInput[Option[Content]]
}

object FilterType extends Enum[FilterType] {

  case object AccountIds extends AccountFilterType with AnalyticModelFilterType with ApiFilterType {
    override type Content = NonEmptySet[AccountId]

    override val input: EndpointInput[Option[Content]] =
      query[Option[NonEmptySet[AccountId]]]("account_ids")

    override def accountFilter(content: NonEmptySet[AccountId]): Account => Boolean =
      account => content.contains(account.id)

    override def modelFilter(content: NonEmptySet[AccountId]): AnalyticOperation => Boolean = operation =>
      content.contains(operation.accountId)
  }

  case object OperationIds extends AnalyticModelFilterType with ApiFilterType {
    override type Content = NonEmptySet[OperationId]

    override def input: EndpointInput[Option[NonEmptySet[OperationId]]] =
      query[Option[NonEmptySet[OperationId]]]("operation_ids")

    override def modelFilter(content: NonEmptySet[OperationId]): AnalyticOperation => Boolean =
      operation => content.contains(operation.id)
  }

  case object CategoryIds extends FilterType with ApiFilterType {
    override type Content = NonEmptySet[CategoryId]

    override def input: EndpointInput[Option[NonEmptySet[CategoryId]]] =
      query[Option[NonEmptySet[CategoryId]]]("category_ids")

    override def filter(content: NonEmptySet[CategoryId]): RichOperation => Boolean =
      rich => content.contains(rich.categoryId)
  }

  case object AccountTypeFilter extends AnalyticModelFilterType with AccountFilterType {
    override type Content = AccountType

    override def accountFilter(content: AccountType): Account => Boolean =
      _.accountType == content

    override def modelFilter(content: AccountType): AnalyticOperation => Boolean =
      _.specificFields.accountType == content
  }

  case object BrandIds extends ModelFilterType with ApiFilterType {
    override type Content = NonEmptySet[BrandId]

    override def input: EndpointInput[Option[NonEmptySet[BrandId]]] =
      query[Option[NonEmptySet[BrandId]]]("brand_ids")

    override def modelFilter(content: NonEmptySet[BrandId]): Operation => Boolean =
      operation => content.contains(operation.specificFields.brandId)
  }

  /**
   * Sometimes we have complex filters which can be partially applied to Operation but for fully logic its has to
   * be applied to RichOperation. PartiallyModelFilterType is introduced for avoiding getting instance ModelFilterApplier
   * (in case we extends it from ModelFilterType). See example Examples#example12
   */
  case object ComplexFilter extends PartiallyModelFilterType {
    override type Content = (BrandId, CategoryId)

    override def modelFilter(content: (BrandId, CategoryId)): Operation => Boolean =
      _.specificFields.brandId == content._1

    override def filter(content: (BrandId, CategoryId)): RichOperation => Boolean =
      operation => operation.model.specificFields.brandId == content._1 && operation.categoryId == content._2
  }

  override def values: IndexedSeq[FilterType] = findValues
}
