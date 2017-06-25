package xyz.casperkoning.poc

import java.util.Properties
import scala.concurrent._
import scala.concurrent.duration._

import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer => JKafkaProducer}
import org.apache.kafka.common.serialization._
import org.apache.kafka.streams._
import xyz.casperkoning.poc.domain._
import xyz.casperkoning.poc.serde._

object ExampleProducer {
  def main(args: Array[String]): Unit = {
    val producerConfig = {
      val props = new Properties
      props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")
      props
    }

    implicit val keySerde = new CaseClassSerde[Key](isKey = true)
    implicit val stringValueSerde = new CaseClassSerde[StringValue](isKey = false)
    implicit val intValueSerde = new CaseClassSerde[IntValue](isKey = false)
    implicit val doubleValueSerde = new CaseClassSerde[DoubleValue](isKey = false)
    implicit val longValueSerde = new CaseClassSerde[LongValue](isKey = false)

    val stringProducer = new KafkaProducer[Key, StringValue](producerConfig)
    val intProducer = new KafkaProducer[Key, IntValue](producerConfig)
    val doubleProducer = new KafkaProducer[Key, DoubleValue](producerConfig)
    val longProducer = new KafkaProducer[Key, LongValue](producerConfig)

    import scala.concurrent.ExecutionContext.Implicits.global

    Await.result(
      Future
        .sequence(List(
          stringProducer.send("strings", Key("1"), StringValue("hello")).map(a => {Thread.sleep(1000L) ; a}),
          intProducer.send("ints", Key("1"), IntValue(10)).map(a => {Thread.sleep(1000L) ; a}),
          doubleProducer.send("doubles", Key("1"), DoubleValue(2.0)).map(a => {Thread.sleep(1000L) ; a}),
          longProducer.send("longs", Key("1"), LongValue(3L))
        ))
        .map(_.foreach(println)),
      10 seconds
    )
  }

  class KafkaProducer[K,V](props: Properties)(implicit keySerde: Serde[K], valSerde: Serde[V]) {
    private val producer = new JKafkaProducer[K,V](props, keySerde.serializer(), valSerde.serializer())
    def send(topic: String, k: K, v: V): Future[String] = {
      val promise = Promise[String]
      val record = new ProducerRecord[K,V](topic, k, v)
      producer.send(record, (metadata, exception) => {
        Option(metadata).foreach(md => promise.success(s"Produced offset ${md.offset} on ${md.topic}"))
        Option(exception).foreach(e => promise.failure(new Exception(s"ERROR while producing: $e")))
      })
      promise.future
    }
  }
}
