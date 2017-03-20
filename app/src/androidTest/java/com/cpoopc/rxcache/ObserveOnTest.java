package com.cpoopc.rxcache;

import android.test.AndroidTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


/**
 * @author cpoopc
 * @date 2016/1/18
 * @time 18:51
 * @description
 */
public class ObserveOnTest extends AndroidTestCase {

    private static class MokeException extends RuntimeException {
        public MokeException(String detailMessage) {
            super(detailMessage);
        }
    }

    public void testObserveOnAndroidMainThread() throws InterruptedException {
        final CountDownLatch completedLatch = new CountDownLatch(1);
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final int count = 20;
        Observable.create(new ObservableOnSubscribe<Object>() {

            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                emitter.onNext(count);
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                subscriber.onError(new MokeException("moke exception"));
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Object value) {
                        android.util.Log.e("cc:", "onNext:" + value);
                        System.out.println("onNext:" + value);
                        atomicInteger.getAndSet(Integer.valueOf((String) value));
                        completedLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("onError:" + atomicInteger.get());
                        android.util.Log.e("cc:", "onError:" + atomicInteger.get());
                        assertEquals(atomicInteger.get(), count);
                        completedLatch.countDown();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        completedLatch.await();
    }
}
