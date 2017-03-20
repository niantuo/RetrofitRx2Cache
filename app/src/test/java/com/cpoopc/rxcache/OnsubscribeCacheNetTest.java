package com.cpoopc.rxcache;

import android.support.annotation.NonNull;

import com.cpoopc.retrofitrxcache.AsyncOnSubscribeCacheNet;
import com.cpoopc.retrofitrxcache.CacheNetOnSubscribeFactory;
import com.cpoopc.retrofitrxcache.OnSubscribeCacheNet;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * @author cpoopc
 * @date 2016/06/29
 * @time 22:53
 * @description
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
@FixMethodOrder(MethodSorters.DEFAULT)
public class OnsubscribeCacheNetTest {

    public final String TAG = "OnsubscribeCacheNetTest";

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testAsyncSequence() throws InterruptedException {
        Observable<String> observableShort = createShortObservable();
        Observable<String> observableLong = createLongObservable();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final List<String> resultList = new ArrayList<>();
        Observable.create(new CacheNetOnSubscribeFactory(false).create(observableShort, observableLong, new Consumer<String>() {

            @Override
            public void accept(String s) throws Exception {
                android.util.Log.d(TAG, "cache action : " + s);
                System.out.println("cache action : " + s);
                Assert.assertNotNull(s);
            }
        })).subscribe(new Observer<String>() {

            @Override
            public void onComplete() {
                android.util.Log.d(TAG, "onCompleted ");
                System.out.println("onCompleted ");
                countDownLatch.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                android.util.Log.d(TAG, "onError : " + e);
                System.out.println("onError : " + e);
                countDownLatch.countDown();
            }

            @Override
            public void onNext(String s) {
                android.util.Log.d(TAG, "onNext : " + s);
                resultList.add(s);
                System.out.println("onNext : " + s);
            }
        });
        countDownLatch.await();
        Assert.assertEquals(2, resultList.size());
        Assert.assertEquals("short", resultList.get(0));
        Assert.assertEquals("long", resultList.get(1));
    }

    @Test
    public void testAsyncReverseSequence() throws InterruptedException {
        Observable<String> observableShort = createShortObservable();
        Observable<String> observableLong = createLongObservable();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final List<String> resultList = new ArrayList<>();
        Observable.create(new AsyncOnSubscribeCacheNet<String>(observableLong, observableShort, new Consumer<String>() {

            @Override
            public void accept(String s) throws Exception {
                android.util.Log.d(TAG, "cache action : " + s);
                System.out.println("cache action : " + s);
                Assert.assertNotNull(s);
            }
        })).subscribe(new Observer<String>() {

            @Override
            public void onComplete() {
                android.util.Log.d(TAG, "onCompleted ");
                System.out.println("onCompleted ");
                countDownLatch.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                android.util.Log.d(TAG, "onError : " + e);
                System.out.println("onError : " + e);
                countDownLatch.countDown();
            }

            @Override
            public void onNext(String s) {
                android.util.Log.d(TAG, "onNext : " + s);
                resultList.add(s);
                System.out.println("onNext : " + s);
            }
        });
        countDownLatch.await();
        Assert.assertEquals(1, resultList.size());// 当网络的先返回,就不会再发射缓存Observable内容
        Assert.assertEquals("short", resultList.get(0));
    }

    @Test
    public void testAsyncOnError() throws InterruptedException {
        Observable<String> errorObservable = Observable.create(new ObservableOnSubscribe<String>() {


            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                emitter.onError(new NullPointerException("测试错误"));
            }
        });
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Observable.create(new CacheNetOnSubscribeFactory(true).create(errorObservable, createShortObservable(), new Consumer<String>() {

            @Override
            public void accept(String s) throws Exception {

            }

        })).subscribe(new Observer<String>() {

            @Override
            public void onComplete() {
                System.out.println("完成");
                countDownLatch.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {

            }


            @Override
            public void onError(Throwable e) {
                Assert.assertNull(e);
            }

            @Override
            public void onNext(String s) {
                System.out.println(s);
                Assert.assertEquals(s, "short");
            }
        });
        countDownLatch.await();

    }

    @Test
    public void testSyncSequence() throws InterruptedException {
        Observable<String> observableShort = createShortObservable();
        Observable<String> observableLong = createLongObservable();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final List<String> resultList = new ArrayList<>();
        Observable.create(new OnSubscribeCacheNet<String>(observableShort, observableLong, new Consumer<String>() {

            @Override
            public void accept(String s) throws Exception {
                android.util.Log.d(TAG, "cache action : " + s);
                System.out.println("cache action : " + s);
                Assert.assertNotNull(s);
            }

        })).subscribe(new Observer<String>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onComplete() {
                android.util.Log.d(TAG, "onCompleted ");
                System.out.println("onCompleted ");
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                android.util.Log.d(TAG, "onError : " + e);
                System.out.println("onError : " + e);
                countDownLatch.countDown();
            }

            @Override
            public void onNext(String s) {
                android.util.Log.d(TAG, "onNext : " + s);
                resultList.add(s);
                System.out.println("onNext : " + s);
            }
        });
        countDownLatch.await();
        Assert.assertEquals(2, resultList.size());
        Assert.assertEquals("short", resultList.get(0));
        Assert.assertEquals("long", resultList.get(1));
    }

    @Test
    public void testSyncReverseSequence() throws InterruptedException {
        Observable<String> observableShort = createShortObservable();
        Observable<String> observableLong = createLongObservable();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final List<String> resultList = new ArrayList<>();
        Observable.create(new OnSubscribeCacheNet<String>(observableLong, observableShort, new Consumer<String>() {

            @Override
            public void accept(String s) throws Exception {
                android.util.Log.d(TAG, "cache action : " + s);
                System.out.println("cache action : " + s);
                Assert.assertNotNull(s);
            }

        })).subscribe(new Observer<String>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onComplete() {
                android.util.Log.d(TAG, "onCompleted ");
                System.out.println("onCompleted ");
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                android.util.Log.d(TAG, "onError : " + e);
                System.out.println("onError : " + e);
                countDownLatch.countDown();
            }

            @Override
            public void onNext(String s) {
                android.util.Log.d(TAG, "onNext : " + s);
                resultList.add(s);
                System.out.println("onNext : " + s);
            }
        });
        countDownLatch.await();
        Assert.assertEquals(2, resultList.size());// 当网络的先返回,就不会再发射缓存Observable内容
        Assert.assertEquals("long", resultList.get(0));
        Assert.assertEquals("short", resultList.get(1));
    }

    @NonNull
    private Observable<String> createLongObservable() {
        return Observable.create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                try {
                    Thread.sleep(2000);
                    emitter.onNext("long");
                    emitter.onComplete();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    emitter.onError(e);
                }
            }
        });
    }

    @NonNull
    private Observable<String> createShortObservable() {
        return Observable.create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                try {
                    Thread.sleep(1000);
                    emitter.onNext("short");
                    emitter.onComplete();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    emitter.onError(e);
                }
            }
        });
    }
}
