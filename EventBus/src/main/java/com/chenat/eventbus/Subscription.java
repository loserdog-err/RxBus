package com.chenat.eventbus;

/**
 * 订阅类的方法的封装对象，在 EventBus 的基础上增加了 subscription 字段，用于在取消注册时取消订阅
 */
final class Subscription {
    final Object subscriber;
    final SubscriberMethod subscriberMethod;
    rx.Subscription subscription;

    Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof Subscription) {
            Subscription otherSubscription = (Subscription) other;
            return subscriber == otherSubscription.subscriber
                    && subscriberMethod.equals(otherSubscription.subscriberMethod);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode();
    }
}