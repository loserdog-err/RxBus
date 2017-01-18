package com.chenat.eventbus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * Created by ChenAt on 2017/1/18.
 * 基于 greenrobot 的 eventBus 使用 RxJava 实现的事件总线
 */

public class EventBus {
    static volatile EventBus defaultInstance;

    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    private final Map<Object, List<Class<?>>> typesBySubscriber;
    private final Map<Class<?>, Object> stickyEvents;

    private SubscriberMethodFinder subscriberMethodFinder;
    private Subject<Object, Object> mBus;
    private boolean mIsLoggable;

    public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }

    private EventBus() {
        stickyEvents = new ConcurrentHashMap<>();
        mBus = new SerializedSubject<>(PublishSubject.create());
        subscriberMethodFinder = new SubscriberMethodFinder(false);
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
    }

    public void post(Object event) {
        mBus.onNext(event);
    }

    public void postSticky(Object event) {
        synchronized (stickyEvents) {
            stickyEvents.put(event.getClass(), event);
        }
        post(event);
    }

    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        Class<?> eventType = subscriberMethod.eventType;
        final Subscription newSubscription = new Subscription(subscriber, subscriberMethod);

        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        subscribedEvents.add(eventType);
        Observable<?> observable = mBus.ofType(eventType);
        //指定订阅者调用的线程
        observeOn(newSubscription, observable);
        //为 subscription 字段赋值，用于稍后取消订阅
        newSubscription.subscription = observable.subscribe(new Action1<Object>() {
            @Override
            public void call(Object event1) {
                invokeSubscriber(newSubscription, event1);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        addSubscriptionToMap(newSubscription, eventType);
        //处理 sticky event
        if (subscriberMethod.sticky) {
            synchronized (stickyEvents) {
                final Object event = stickyEvents.get(eventType);
                if (event != null) {
                    invokeSubscriber(newSubscription, event);
                }
            }
        }
    }

    private void addSubscriptionToMap(Subscription subscription, Class<?> eventType) {
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType, subscriptions);
        } else {
            if (subscriptions.contains(subscription)) {
                return;
            }
        }
        subscriptions.add(subscription);
    }

    private <T> Observable<T> toObservable(boolean isSticky, final Class<T> eventType) {
        return mBus.ofType(eventType);
    }

    private void observeOn(Subscription subscription, Observable observable) {
        switch (subscription.subscriberMethod.threadMode) {
            case POSTING:
                break;
            case MAIN:
                observable.observeOn(AndroidSchedulers.mainThread());
                break;
            case BACKGROUND:
                observable.observeOn(Schedulers.io());
                break;
            case ASYNC:
                observable.observeOn(Schedulers.newThread());
                break;
            default:
                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
        }
    }

    private void invokeSubscriber(Subscription subscription, Object event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> T removeStickyEvent(Class<T> eventType) {
        synchronized (stickyEvents) {
            return eventType.cast(stickyEvents.remove(eventType));
        }
    }

    public boolean removeStickyEvent(Object event) {
        synchronized (stickyEvents) {
            Class<?> eventType = event.getClass();
            Object existingEvent = stickyEvents.get(eventType);
            if (event.equals(existingEvent)) {
                stickyEvents.remove(eventType);
                return true;
            } else {
                return false;
            }
        }
    }

    public void removeAllStickyEvents() {
        synchronized (stickyEvents) {
            stickyEvents.clear();
        }
    }

    public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

    public void unregister(Object subscriber) {
        List<Class<?>> subscribeEventTypes = typesBySubscriber.get(subscriber);
        if (subscribeEventTypes == null) {
            return;
        }
        for (Class<?> eventType : subscribeEventTypes) {
            unregisterByEventType(eventType, subscriber);
        }
        typesBySubscriber.remove(subscriber);
    }

    private void unregisterByEventType(Class<?> eventType, Object subscriber) {
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        for (Subscription subscription : subscriptions) {
            if (!subscription.subscription.isUnsubscribed() && subscription.subscriber == subscriber) {
                subscription.subscription.unsubscribe();
                subscriptions.remove(subscription);
            }
        }
    }
}
