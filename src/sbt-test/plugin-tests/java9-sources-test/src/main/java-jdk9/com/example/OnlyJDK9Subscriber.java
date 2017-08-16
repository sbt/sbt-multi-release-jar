package com.example;

import java.util.concurrent.Flow;

public class OnlyJDK9Subscriber implements Flow.Subscriber<String> {
  
  @Override
  public void onSubscribe(Flow.Subscription subscription) {
    subscription.cancel(); 
  }

  @Override
  public void onNext(String item) {
    // noop
  }

  @Override
  public void onError(Throwable throwable) {
    // noop
  }

  @Override
  public void onComplete() {
    // noop
  }
}
