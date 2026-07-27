package net.cheltsov

import zio.prelude.Subtype

package object model {
  type OperationId = OperationId.Type
  object OperationId extends Subtype[String]

  type AccountId = AccountId.Type
  object AccountId extends Subtype[String]

  type BrandId = BrandId.Type
  object BrandId extends Subtype[String]

  type CategoryId = CategoryId.Type
  object CategoryId extends Subtype[String]
}
