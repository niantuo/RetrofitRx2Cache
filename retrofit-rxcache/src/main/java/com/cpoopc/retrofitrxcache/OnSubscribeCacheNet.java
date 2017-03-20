package com.cpoopc.retrofitrxcache;


import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * 缓存+网络请求
 * User: cpoopc
 * Date: 2016-01-18
 * Time: 10:19
 * Ver.: 0.1
 */
public class OnSubscribeCacheNet<T> implements ObservableOnSubscribe<T> {

    private final boolean debug = false;
    private final String TAG = "OnSubscribeCacheNet:";

    // 读取缓存
    private ObservableWrapper<T> cacheObservable;

    // 读取网络
    private ObservableWrapper<T> netObservable;

    // 网络读取完毕,缓存动作
    private Consumer<T> storeCacheAction;

    private int stepIndex;
    private final int finalStepIndex;
    private AtomicInteger overCount = new AtomicInteger(0);
    private boolean sync = true;// true 同步,false:异步
    private CountDownLatch cacheLatch = new CountDownLatch(1);

    public OnSubscribeCacheNet(Observable<T> cacheObservable, Observable<T> netObservable, Consumer<T> storeCacheAction) {
        if (cacheObservable != null) {
            this.cacheObservable = new ObservableWrapper<T>(1, cacheObservable);
        }
        this.netObservable = new ObservableWrapper<T>(2, netObservable);
        this.storeCacheAction = storeCacheAction;
        finalStepIndex = 2;
    }

    @Override
    public void subscribe(ObservableEmitter<T> emitter) throws Exception {
        if (cacheObservable != null) {
            if (sync) {
                cacheObservable.getObservable().subscribe(new CacheNetObserver<T>(cacheObservable, emitter, storeCacheAction));
            } else {
                cacheObservable.getObservable().subscribeOn(Schedulers.io()).subscribe(new CacheNetObserver<T>(cacheObservable, emitter, storeCacheAction));
            }
        }
        if (sync) {
            netObservable.getObservable().subscribe(new CacheNetObserver<T>(netObservable, emitter, storeCacheAction));
        } else {
            netObservable.getObservable().subscribeOn(Schedulers.newThread()).subscribe(new CacheNetObserver<T>(netObservable, emitter, storeCacheAction));
        }
    }


    class ObservableWrapper<T> {

        int index;
        Observable<T> observable;
        T data;

        public ObservableWrapper(int index, Observable<T> observable) {
            this.index = index;
            this.observable = observable;
        }

        public int getIndex() {
            return index;
        }

        public Observable<T> getObservable() {
            return observable;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }

    class CacheNetObserver<T> implements Observer<T> {

        ObservableWrapper<T> observableWrapper;
        ObservableEmitter<? super T> subscriber;
        Consumer<T> storeCacheAction;

        public CacheNetObserver(ObservableWrapper<T> observableWrapper, ObservableEmitter<? super T> subscriber, Consumer<T> storeCacheAction) {
            this.observableWrapper = observableWrapper;
            this.subscriber = subscriber;
            this.storeCacheAction = storeCacheAction;
        }

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {
            if (cacheObservable != null) {

                if (sync) {
                    cacheObservable.getObservable()
                            .subscribe(new CacheNetObserver(cacheObservable,subscriber,storeCacheAction));
                } else {
                    cacheObservable.getObservable()
                            .subscribeOn(Schedulers.io())
                            .subscribe(new CacheNetObserver(cacheObservable, subscriber, storeCacheAction));
                }
            }
            if (sync) {
                netObservable.getObservable()
                        .subscribe(new CacheNetObserver(netObservable, subscriber, storeCacheAction));
            } else {
                netObservable.getObservable()
                        .subscribeOn(Schedulers.newThread())
                        .subscribe(new CacheNetObserver(netObservable, subscriber, storeCacheAction));
            }
        }

        @Override
        public void onError(Throwable e) {
            if (debug) Log.e(TAG, "onError stepIndex:" + observableWrapper.getIndex());
            overCount.addAndGet(1);
            if (finalStepIndex == observableWrapper.getIndex()) {
                while (overCount.get() < finalStepIndex) {
                    if (debug)
                        Log.e(TAG, "onError stepIndex:" + observableWrapper.getIndex() + " overCount.get() < finalStepIndex");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                if (!sync) {
                    try {
                        cacheLatch.await();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
//                    try {// FIXME: 2016/1/18 issue A 临时解决办法
//                        Thread.sleep(500);
//                    } catch (InterruptedException e1) {
//                        e1.printStackTrace();
//                    }
                }
                if (subscriber != null && !subscriber.isDisposed()) {
                    subscriber.onError(e);
                }
            }
        }

        @Override
        public void onNext(T o) {
            if (debug)
                Log.i(TAG, "stepIndex :" + stepIndex + " observableWrapper.getIndex():" + observableWrapper.getIndex() + " o:" + o);
            observableWrapper.setData(o);
            if (stepIndex < observableWrapper.getIndex()) {
                stepIndex = observableWrapper.getIndex();
                if (debug)
                    Log.i(TAG, "stepIndex :" + stepIndex + " observableWrapper.getIndex():" + observableWrapper.getIndex());
                logThread("");
                if (debug)
                    Log.e(TAG, " check subscriber :" + subscriber + " isUnsubscribed:" + subscriber.isDisposed());
                if (subscriber != null && !subscriber.isDisposed()) {
                    subscriber.onNext(o);// FIXME: issue A:外面如果ObservableOn 其他线程,将会是异步操作,如果在实际的onNext调用之前,发生了onComplete或者onError,将会unsubscribe,导致onNext没有被调用
                }
            }
        }
    }

    public void logThread(String tag) {
        if (debug) Log.i(TAG, tag + " : " + Thread.currentThread().getName());
    }

}
