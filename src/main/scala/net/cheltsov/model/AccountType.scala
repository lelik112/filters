package net.cheltsov.model

sealed trait AccountType

object AccountType {
  case object Prime   extends AccountType
  case object Deposit extends AccountType
}
