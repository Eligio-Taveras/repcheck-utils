package com.repcheck.utils.doobie

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.repcheck.utils.doobie.DoobieArrayCodecs._

class DoobieArrayCodecsSpec extends AnyFlatSpec with Matchers {

  "string array conversions" should "round-trip" in {
    val list = List("a", "b")
    DoobieArrayCodecs.arrayToList(DoobieArrayCodecs.listToArray(list)) shouldBe list
  }

  it should "handle empty lists" in {
    DoobieArrayCodecs.arrayToList(DoobieArrayCodecs.listToArray(List.empty[String])) shouldBe List.empty
  }

  "int array conversions" should "round-trip" in {
    val list = List(1, 2, 3)
    DoobieArrayCodecs.intArrayToList(DoobieArrayCodecs.intListToArray(list)) shouldBe list
  }

  "doobie Get/Put instances" should "be derivable for List[String] and List[Int]" in {
    listStringGet.toString should not be empty
    listStringPut.toString should not be empty
    listIntGet.toString should not be empty
    listIntPut.toString should not be empty
  }

}
