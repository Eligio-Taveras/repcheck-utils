package com.repcheck.utils.doobie

import doobie.postgres.implicits._
import doobie.{Get, Put}

object DoobieArrayCodecs {

  private[doobie] val arrayToList: Array[String] => List[String] = _.toList

  private[doobie] val listToArray: List[String] => Array[String] = _.toArray

  implicit val listStringGet: Get[List[String]] =
    Get[Array[String]].map(arrayToList)

  implicit val listStringPut: Put[List[String]] =
    Put[Array[String]].contramap(listToArray)

  private[doobie] val intArrayToList: Array[Int] => List[Int] = _.toList

  private[doobie] val intListToArray: List[Int] => Array[Int] = _.toArray

  implicit val listIntGet: Get[List[Int]] =
    Get[Array[Int]].map(intArrayToList)

  implicit val listIntPut: Put[List[Int]] =
    Put[Array[Int]].contramap(intListToArray)

}
