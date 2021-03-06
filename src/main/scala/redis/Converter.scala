package redis

import akka.util.ByteString
import redis.protocol._
import scala.util.Try
import scala.annotation.{implicitNotFound, tailrec}
import scala.collection.mutable

trait MultiBulkConverter[A] {
  def to(redisReply: MultiBulk): Try[A]
}

object MultiBulkConverter {

  def toSeqString(reply: MultiBulk): Seq[String] = {
    reply.responses.map(r => {
      r.map(_.toString)
    }).get
  }

  def toSeqByteString(reply: MultiBulk): Seq[ByteString] = {
    reply.responses.map(r => {
      r.map(_.toByteString)
    }).get
  }

  def toSeqOptionByteString(reply: MultiBulk): Seq[Option[ByteString]] = {
    reply.responses.map(r => {
      r.map(_.asOptByteString)
    }).get
  }

  def toSeqTuple2ByteStringDouble(reply: MultiBulk): Seq[(ByteString, Double)] = {
    reply.responses.map { r => {
      val s = r.map(_.toByteString)
      val builder = Seq.newBuilder[(ByteString, Double)]
      s.grouped(2).foreach { case Seq(a, b) => builder += ((a, b.utf8String.toDouble)) }
      builder.result()
    }}.get
  }

  def toSeqMapString(reply: MultiBulk): Seq[Map[String, String]] = {
    reply.responses.map { s =>
          s.map ({
            case m: MultiBulk => {
              m.responses.map { s =>
                val builder = Seq.newBuilder[(String, String)]
                s.grouped(2).foreach { case Seq(a, b) => builder += ((a.toString, b.toString)) }
                builder.result()
              }.getOrElse(Seq())
            }
            case _ => Seq()
          }).map { _.toMap }
      }.get
  }

  def toOptionStringByteString(reply: MultiBulk): Option[(String, ByteString)] = {
    reply.responses.map(r => {
      Some(r.head.toString, r.tail.head.toByteString)
    }).getOrElse(None)
  }

  def toSeqBoolean(reply: MultiBulk): Seq[Boolean] = {
    reply.responses.map(r => {
      r.map(_.toString == "1")
    }).get
  }

}

@implicitNotFound(msg = "No ByteString serializer found for type ${T}. Try to implement an implicit ByteStringSerializer for this type.")
trait ByteStringSerializer[K] {
  def serialize(key: K): ByteString
}

object ByteStringSerializer extends ByteStringSerializerLowPriority

trait ByteStringSerializerLowPriority {

  implicit object String extends ByteStringSerializer[String] {
    def serialize(key: String): ByteString = ByteString(key)
  }

  implicit object ShortConverter extends ByteStringSerializer[Short] {
    def serialize(i: Short): ByteString = ByteString(i.toString)
  }

  implicit object IntConverter extends ByteStringSerializer[Int] {
    def serialize(i: Int): ByteString = ByteString(i.toString)
  }

  implicit object LongConverter extends ByteStringSerializer[Long] {
    def serialize(i: Long): ByteString = ByteString(i.toString)
  }

  implicit object FloatConverter extends ByteStringSerializer[Float] {
    def serialize(f: Float): ByteString = ByteString(f.toString)
  }

  implicit object DoubleConverter extends ByteStringSerializer[Double] {
    def serialize(d: Double): ByteString = ByteString(d.toString)
  }

  implicit object CharConverter extends ByteStringSerializer[Char] {
    def serialize(c: Char): ByteString = ByteString(c)
  }

  implicit object ByteConverter extends ByteStringSerializer[Byte] {
    def serialize(b: Byte): ByteString = ByteString(b)
  }

  implicit object ArrayByteConverter extends ByteStringSerializer[Array[Byte]] {
    def serialize(b: Array[Byte]): ByteString = ByteString(b)
  }

  implicit object ByteStringConverter extends ByteStringSerializer[ByteString] {
    def serialize(bs: ByteString): ByteString = bs
  }

}
