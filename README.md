# RxBus
根据 greenrobot 的 EventBus 修改使用 RxJava 实现的事件总线
 
# 前言
公司项目有需求比较适合用 EventBus 来实现，考虑到项目已经引入了 RxJava，所以就不打算引入新的库，直接使用基于 RxJava 的 RxBus 来实现。网上寻觅了一圈，发现基本都是个比较简单的 demo，并不支持我需要的 sticky event，并且或多或少都有 bug，遂自己造了个轮子。考虑到我以前的使用习惯， api 全部照搬 EventBus3.0。EventBus3.0 默认使用反射获得方法，也支持编译时注解，这里我只实现了前者，并没有实现后者。<br>
#此库跟 EventBus 的联系：
* 提供了 EventBus3.0 大部分的 api 
* 复制修改了 EventBus3.0 反射查找方法的类，更高效 <br>

# 支持功能：
* sticky 事件
* 注解
* 跟 EventBus 完全相同的 api

# 此库适用人群：
仅适合项目已集成 RxJava 的开发者，否则最好用 EventBus，有如下原因：
* 从体积上来说，EventBus 本来就很小，如果项目本身没有 RxJava 依赖的话，徒增这些库而只为了实现 RxBus 只会使体积更大。
* EventBus 毕竟是久经沙场的经得起时间考验的，单从它的单元测试就能看出来工程的严谨。 <br><br>

当然，如果项目已集成 RxJava，那么不要考虑，直接用吧，毕竟 EventBus 内部又多了个线程池，相比直接使用 RxJava 的线程调度，多耗了点资源不是？使用 RxJava 同时也更方便你做扩展，还是可以考虑的。

# 使用：
直接将 EventBus 的文档复制过来，毕竟使用方法一模一样，EventBus in 3 steps

1. Define events:

    ```java  
    public static class MessageEvent { /* Additional fields if needed */ }
    ```

2. Prepare subscribers:
    Declare and annotate your subscribing method, optionally specify a [thread mode](http://greenrobot.org/eventbus/documentation/delivery-threads-threadmode/):  

    ```java
    @Subscribe(threadMode = ThreadMode.MAIN)  
    public void onMessageEvent(MessageEvent event) {/* Do something */};
    ```
    Register and unregister your subscriber. For example on Android, activities and fragments should usually register according to their life cycle:

   ```java
    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }
 
    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
    ```

3. Post events:

   ```java
    EventBus.getDefault().post(new MessageEvent());
    ```

Thanks
------
EventBus https://github.com/greenrobot/EventBus
