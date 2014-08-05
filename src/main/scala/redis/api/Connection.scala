package redis.api.connection

import redis._
import akka.util.ByteString
import redis.protocol.Status

case class Auth[V](value: V)(implicit convert: ByteStringSerializer[V]) extends RedisCommandStatus[Status] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("AUTH", Seq(convert.serialize(value)))

  def decodeReply(s: Status) = s
}
