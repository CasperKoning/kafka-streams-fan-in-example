package xyz.casperkoning.poc

import java.util.Properties

import org.apache.kafka.clients.consumer._
import org.apache.kafka.streams._
import org.apache.kafka.streams.kstream._
import domain._
import serde._
import shapeless.Coproduct
import xyz.casperkoning.poc.domain.MergeParts._

object FanInExample {
  def main(args: Array[String]): Unit = {
    val streamingConfig = {
      val props = new Properties
      props.put(StreamsConfig.APPLICATION_ID_CONFIG, "fan-in-example")
      props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")
      props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "8")
      props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "100")
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      props
    }

    val keySerde = new CaseClassSerde[Key](isKey = true)
    val stringValueSerde = new CaseClassSerde[StringValue](isKey = false)
    val intValueSerde = new CaseClassSerde[IntValue](isKey = false)
    val doubleValueSerde = new CaseClassSerde[DoubleValue](isKey = false)
    val longValueSerde = new CaseClassSerde[LongValue](isKey = false)
    val mergePartSerde = new CaseClassSerde[MergePart](isKey = false)
    val mergePartsSerde = new CaseClassSerde[MergeParts](isKey = false)
    val mergeResultValueSerde = new CaseClassSerde[MergeResult](isKey = false)

    val builder = new KStreamBuilder

    val strings = builder.stream(keySerde, stringValueSerde, "strings")
      .mapValues[MergePart] { s => MergePart(MergeParts.StringPartType, Coproduct[MergePartTypes](s)) }

    val ints = builder.stream(keySerde, intValueSerde, "ints")
      .mapValues[MergePart] { i => MergePart(MergeParts.IntPartType, Coproduct[MergePartTypes](i)) }

    val doubles = builder.stream(keySerde, doubleValueSerde, "doubles")
      .mapValues[MergePart] { d => MergePart(MergeParts.DoublePartType, Coproduct[MergePartTypes](d)) }

    val longs = builder.stream(keySerde, longValueSerde, "longs")
      .mapValues[MergePart] { c => MergePart(MergeParts.LongPartType, Coproduct[MergePartTypes](c)) }

    builder
      .merge(strings, ints, doubles, longs)
      .groupByKey(keySerde, mergePartSerde)
      .aggregate[MergeParts](
        () => MergeParts(parts = Map.empty),
        // (key, part, aggregate) => aggregate.upsert(part), FIXME: Using this lambda instead of the anonymous inner class doesn't compile
        new Aggregator[Key, MergePart, MergeParts] {
          override def apply(key: Key, part: MergePart, aggregate: MergeParts): MergeParts = aggregate.upsertPart(part)
        },
        mergePartsSerde,
        "merge-state"
      )
      .filter((_, mergeParts) => mergeParts.containsAllRequiredParts)
      .mapValues[MergeResult](_.mergeToResult)
      .to(keySerde, mergeResultValueSerde, "merge-results")

    val streams = new KafkaStreams(builder, streamingConfig)
    streams.start()
  }

}
