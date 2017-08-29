package com.example

import java.util.concurrent.Flow

class OnlyJDK9SubscriberInScala extends Flow.Subscriber[String] {
  override def onError(throwable: Throwable): Unit = ()

  override def onComplete(): Unit = ()

  override def onNext(item: String): Unit = println(item)

  override def onSubscribe(subscription: Flow.Subscription): Unit = subscription.cancel()

}
