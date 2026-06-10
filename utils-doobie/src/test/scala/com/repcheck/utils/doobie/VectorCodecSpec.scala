package com.repcheck.utils.doobie

import io.circe.syntax._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.repcheck.utils.doobie.VectorCodec._

class VectorCodecSpec extends AnyFlatSpec with Matchers {

  "parseFloatVector" should "parse a pgvector literal into floats" in {
    VectorCodec.parseFloatVector("[1.0, 2.5,3]").toList shouldBe List(1.0f, 2.5f, 3.0f)
  }

  it should "parse an empty vector literal to an empty array" in {
    VectorCodec.parseFloatVector("[]").toList shouldBe List.empty
  }

  "formatFloatVector" should "render the pgvector literal round-trippably" in {
    val arr = Array(1.0f, 2.5f)
    VectorCodec.parseFloatVector(VectorCodec.formatFloatVector(arr)).toList shouldBe arr.toList
  }

  it should "render an empty array as []" in {
    VectorCodec.formatFloatVector(Array.empty[Float]) shouldBe "[]"
  }

  "circe codecs" should "round-trip a float array through JSON" in {
    val arr = Array(0.5f, -1.25f)
    arr.asJson.as[Array[Float]].map(_.toList) shouldBe Right(arr.toList)
  }

  "doobie Get/Put" should "be derivable for Array[Float]" in {
    floatArrayGet.toString should not be empty
    floatArrayPut.toString should not be empty
  }

}
