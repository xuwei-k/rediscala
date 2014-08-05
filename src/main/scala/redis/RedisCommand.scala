package redis

import akka.util.ByteString
import redis.protocol._

trait RedisCommand[RedisReplyT <: RedisReply, T] {
  val isMasterOnly: Boolean
  val encodedRequest: ByteString

  def decodeReply(r: RedisReplyT): T

  val decodeRedisReply: PartialFunction[ByteString, Option[(RedisReplyT, ByteString)]]

  def encode(command: String) = RedisProtocolRequest.inline(command)

  def encode(command: String, args: Seq[ByteString]) = RedisProtocolRequest.multiBulk(command, args)
}


trait RedisCommandStatus[T] extends RedisCommand[Status, T] {
  val decodeRedisReply: PartialFunction[ByteString, Option[(Status, ByteString)]] = RedisProtocolReply.decodeReplyStatus
}

