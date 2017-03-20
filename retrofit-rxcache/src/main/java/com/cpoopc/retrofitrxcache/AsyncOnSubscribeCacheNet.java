package com.cpoopc.retrofitrxcache;


import android.util.Log;

import java.util.concurrent.CountDownLatch;

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
public class AsyncOnSubscribeCacheNet<T> implements ObservableOnSubscribe<T> {

    private final boolean debug = true;
    private final String TAG = "OnSubscribeCacheNet:";

    // 读取缓存
    protected ObservableWrapper<T> cacheObservable;

    // 读取网络
    protected ObservableWrapper<T> netObservable;

    // 网络读取完毕,缓存动作
    protected Consumer<T> storeCacheAction;

    protected CountDownLatch cacheLatch = new CountDownLatch(1);
    protected CountDownLatch netLatch = new CountDownLatch(1);

    public AsyncOnSubscribeCacheNet(Observable<T> cacheObservable, Observable<T> netObservable, Consumer<T> storeCacheAction) {
        this.cacheObservable = new ObservableWrapper<T>(cacheObservable);
        this.netObservable = new ObservableWrapper<T>(netObservable);
        this.storeCacheAction = storeCacheAction;
    }

    @Override
    public void subscribe(ObservableEmitter<T> emitter) throws Exception {
        cacheObservable.getObservable().subscribeOn(Schedulers.io()).subscribe(new CacheObserver<T>(cacheObservable, emitter, storeCacheAction));
        netObservable.getObservable().subscribeOn(Schedulers.io()).subscribe(new NetObserver<T>(netObservable, emitter, storeCacheAction));
    }


    class ObservableWrapper<T> {

        Observable<T> observable;
        T data;

        public ObservableWrapper(Observable<T> observable) {
            this.observable = observable;
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

    class NetObserver<T> implements Observer<T> {

        ObservableWrapper<T> observableWrapper;
        ObservableEmitter<? super T> subscriber;
        Consumer<T> storeCacheAction;

        public NetObserver(ObservableWrapper<T> observableWrapper, ObservableEmitter<? super T> subscriber, Consumer<T> storeCacheAction) {
            this.observableWrapper = observableWrapper;
            this.subscriber = subscriber;
            this.storeCacheAction = storeCacheAction;
        }

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {
            if (debug) Log.i(TAG, "net onCompleted ");
            try {
                if (storeCacheAction != null) {
                    logThread("保存到本地缓存 ");
                    storeCacheAction.accept(observableWrapper.getData());
                }
            } catch (Exception e) {
                onError(e);
            }
            if (subscriber != null && !subscriber.isDisposed()) {
                subscriber.onComplete();
            }
            netLatch.countDown();
        }

        @Override
        public void onError(Throwable e) {
            if (debug) Log.e(TAG, "net onError ");
            try {
                if (debug) Log.e(TAG, "net onError await if cache not completed.");
                cacheLatch.await();
                if (debug) Log.e(TAG, "net onError await over.");
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            if (subscriber != null && !subscriber.isDisposed()) {
                subscriber.onError(e);
            }
        }

        @Override
        public void onNext(T o) {
            if (debug) Log.i(TAG, "net onNext o:" + o);
            observableWrapper.setData(o);
            logThread("");
            if (debug)
                Log.e(TAG, " check subscriber :" + subscriber + " isUnsubscribed:" + subscriber.isDisposed());
            if (subscriber != null && !subscriber.isDisposed()) {
                subscriber.onNext(o);// FIXME: issue A:外面如果ObservableOn 其他线程,将会是异步操作,如果在实际的onNext调用之前,发生了onComplete或者onError,将会unsubscribe,导致onNext没有被调用
            }
        }
    }

    class CacheObserver<T> implements Observer<T> {

        ObservableWrapper<T> observableWrapper;
        ObservableEmitter<? super T> subscriber;
        Consumer<T> storeCacheAction;

        public CacheObserver(ObservableWrapper<T> observableWrapper, ObservableEmitter<? super T> subscriber, Consumer<T> storeCacheAction) {
            this.observableWrapper = observableWrapper;
            this.subscriber = subscriber;
            this.storeCacheAction = storeCacheAction;
        }

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {
            if (debug) Log.i(TAG, "cache onCompleted");
            cacheLatch.countDown();
        }

        @Override
        public void onError(Throwable e) {
            if (debug) Log.e(TAG, "cache onError");
            Log.e(TAG, "read cache error:" + e.getMessage());
            if (debug) e.printStackTrace();
            cacheLatch.countDown();
        }

        @Override
        public void onNext(T o) {
            if (debug) Log.i(TAG, "cache onNext o:" + o);
            observableWrapper.setData(o);
            logThread("");
            if (netLatch.getCount() > 0) {
                if (debug)
                    Log.e(TAG, " check subscriber :" + subscriber + " isUnsubscribed:" + subscriber.isDisposed());
                if (subscriber != null && !subscriber.isDisposed()) {
                    subscriber.onNext(o);// FIXME: issue A:外面如果ObservableOn 其他线程,将会是异步操作,如果在实际的onNext调用之前,发生了onComplete或者onError,将会unsubscribe,导致onNext没有被调用
                }
            } else {
                if (debug) Log.e(TAG, "net result had been load,so cache is not need to load");
            }
        }
    }

    public void logThread(String tag) {
        if (debug) Log.i(TAG, tag + " : " + Thread.currentThread().getName());
    }

}
