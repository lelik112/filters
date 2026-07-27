package net.cheltsov

import zio.UIO

import net.cheltsov.filters.FilterType._
import net.cheltsov.filters.{Filter, FilterInput}
import net.cheltsov.model.{AnalyticOperation, AnalyticRichOperation, Operation, RichOperation}

import shapeless.Generic
import sttp.tapir.EndpointInput

object Examples {

  val service: OperationsService = OperationsService()

  // Returns rich operations using an endpoint filter case class.
  val example1: UIO[List[RichOperation]] =
    service.richOperations(OperationFilters.Empty)

  // Returns rich operations using a tuple of filters.
  val example2: UIO[List[RichOperation]] =
    service.richOperations(AccountTypeFilter.empty -> CategoryIds.empty)

  // Returns rich operations using a nested combination of tuples and filter case classes.
  val example3: UIO[List[RichOperation]] =
    service.richOperations(OperationFilters.Empty -> (AccountTypeFilter.empty -> CategoryIds.empty))

  // Returns operations using filters that can be applied before rich enrichment.
  val example4: UIO[List[Operation]] =
    service.operations(AccountTypeFilter.empty -> BrandIds.empty)

  /**
   * Does not compile because CategoryIds can only be applied to RichOperation.
   * Therefore, ModelFiltersProduct cannot be derived for this argument.
   */
//  val example5: UIO[List[Operation]] = service.operations(CategoryIds.empty)

  /**
   * RichFiltersProduct extends ModelFiltersProduct so that OperationsService can reuse the same
   * account- and model-level filtering pipeline for both plain and rich operations.
   *
   * Importing RichFiltersProduct instances explicitly also makes it possible to pass a rich-only
   * filter to operations. In that case, the filter is ignored at the model stage because it can
   * only be evaluated after rich enrichment. This is an intentional trade-off of the shared design.
   */
  val example6Warn: UIO[List[Operation]] = {
    import net.cheltsov.filters.RichFiltersProduct._

    service.operations(CategoryIds.empty)
  }

  // Returns rich analytical operations using an analytics endpoint filter case class.
  val example7: UIO[List[AnalyticRichOperation]] =
    service.analyticRichOperations(AnalyticFilters.Empty)

  /**
   * Does not compile because BrandIds is not available in the analytical pipeline.
   * Therefore, RichFiltersProduct cannot be derived for this argument.
   */
//  val example8: UIO[List[AnalyticRichOperation]] = service.analyticRichOperations(BrandIds.empty)

  // Returns analytical operations using filters that can be applied to the analytical model.
  val example9: UIO[List[AnalyticOperation]] =
    service.analyticOperations(AccountTypeFilter.empty -> AccountIds.empty)

  /**
   * Does not compile because BrandIds cannot be applied to AnalyticOperation.
   * Therefore, ModelFiltersProduct cannot be derived for this combination.
   */
//  val example10: UIO[List[AnalyticOperation]] = service.analyticOperations(AccountTypeFilter.empty -> BrandIds.empty)

  // Returns rich operations using a filter that is evaluated in both model and rich stages.
  val example11: UIO[List[RichOperation]] =
    service.richOperations(ComplexFilter.empty)

  /**
   * Does not compile because ComplexFilter requires the rich stage for its complete evaluation.
   * Therefore, ModelFiltersProduct cannot be derived for this argument.
   */
//  val example12: UIO[List[Operation]] = service.operations(ComplexFilter.empty)

  // Filters exposed by the rich-operations endpoint. Every field must be both FilterType and ApiFilterType.
  final case class OperationFilters(
    accountIds:   Filter[AccountIds.type] = Filter.empty,
    operationIds: Filter[OperationIds.type] = Filter.empty,
    categoryIds:  Filter[CategoryIds.type] = Filter.empty,
    brandIds:     Filter[BrandIds.type] = Filter.empty
  )

  object OperationFilters {
    val Empty: OperationFilters = OperationFilters()

    val Input: EndpointInput[OperationFilters] =
      FilterInput
        .input(Generic[OperationFilters].to(Empty))
        .map(Generic[OperationFilters].from(_))(Generic[OperationFilters].to(_))
  }

  // Filters exposed by the analytical rich-operations endpoint.
  // Every field must be both AnalyticFilterType and ApiFilterType.
  final case class AnalyticFilters(
    accountIds:   Filter[AccountIds.type] = Filter.empty,
    operationIds: Filter[OperationIds.type] = Filter.empty
  )

  object AnalyticFilters {
    val Empty: AnalyticFilters = AnalyticFilters()

    val Input: EndpointInput[AnalyticFilters] =
      FilterInput
        .input(Generic[AnalyticFilters].to(Empty))
        .map(Generic[AnalyticFilters].from(_))(Generic[AnalyticFilters].to(_))
  }

  final case class NonApiFilters(accountType: Filter[AccountTypeFilter.type] = Filter.empty)

  object NonApiFilters {
    val Empty: NonApiFilters = NonApiFilters()

    /**
     * Does not compile because AccountTypeFilter is an internal filter and is not an ApiFilterType.
     * Therefore, FilterInput cannot derive a Tapir endpoint input for this case class.
     */
//    val Input: EndpointInput[NonApiFilters] =
//      FilterInput
//        .input(Generic[NonApiFilters].to(Empty))
//        .map(Generic[NonApiFilters].from(_))(Generic[NonApiFilters].to(_))
  }
}
