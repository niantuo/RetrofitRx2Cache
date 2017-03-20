package com.cpoopc.retrofitrxcache;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

/**
 * User: cpoopc
 * Date: 2016/05/15
 * Time: 12:58
 */
public class SyncOnSubscribeCacheNet<T> extends AsyncOnSubscribeCacheNet<T> {
    public SyncOnSubscribeCacheNet(Observable<T> cacheObservable, Observable<T> netObservable, Consumer<T> storeCacheAction) {
        super(cacheObservable, netObservable, storeCacheAction);
    }


    @Override
    public void subscribe(ObservableEmitter<T> emitter) throws Exception {
        cacheObservable.getObservable().subscribe(new CacheObserver<T>(cacheObservable, emitter, storeCacheAction));
        netObservable.getObservable().subscribe(new NetObserver<T>(netObservable, emitter, storeCacheAction));
    }

}
