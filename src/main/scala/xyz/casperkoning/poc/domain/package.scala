package xyz.casperkoning.poc

import shapeless.{:+:, CNil}
import xyz.casperkoning.poc.domain.MergeParts._

package object domain {
  case class Key(key: String)

  case class StringValue(value: String)

  case class IntValue(value: Int)

  case class DoubleValue(value: Double)

  case class LongValue(value: Long)

  case class MergePart(key: MergePartKey, value: MergePartTypes)

  object MergeParts {
    type MergePartTypes = StringValue :+: IntValue :+: DoubleValue :+: LongValue :+: CNil
    type MergePartKey = String

    // required parts
    val StringPartType: MergePartKey = classOf[String].getSimpleName
    val IntPartType: MergePartKey = classOf[Int].getSimpleName
    val DoublePartType: MergePartKey = classOf[Double].getSimpleName

    val RequiredParts: Seq[MergePartKey] = Seq(StringPartType, IntPartType, DoublePartType)

    // optional parts
    val LongPartType: MergePartKey = classOf[Long].getSimpleName
  }

  case class MergeParts(parts: Map[MergePartKey, MergePart]) {
    private def getRequiredPart(mergePartKey: MergePartKey): MergePart =
      parts.getOrElse(mergePartKey, sys.error(s"Did not find a part of type $mergePartKey, while the required parts are $RequiredParts. Did you forget to filter on `containsAllRequiredParts`?"))

    private def getOptionalPart(mergePartType: MergePartKey): Option[MergePart] =
      parts.get(mergePartType)

    def upsertPart(part: MergePart): MergeParts = this.copy(parts = parts.updated(part.key, part))

    def mergeToResult: MergeResult = MergeResult(
      //required fields
      value1 = getRequiredPart(StringPartType).value.select[StringValue].map(_.value).get,
      value2 = getRequiredPart(IntPartType).value.select[IntValue].map(_.value).get,
      value3 = getRequiredPart(DoublePartType).value.select[DoubleValue].map(_.value).get,
      // optional fields
      value4 = getOptionalPart(LongPartType).flatMap(_.value.select[LongValue].map(_.value))
    )

    def containsAllRequiredParts: Boolean = RequiredParts.forall(parts.contains)
  }

  case class MergeResult(value1: String, value2: Int, value3: Double, value4: Option[Long] = None)
}
