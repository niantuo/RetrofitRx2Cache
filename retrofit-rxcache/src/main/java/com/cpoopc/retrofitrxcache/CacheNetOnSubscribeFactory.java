package com.cpoopc.retrofitrxcache;


import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;

/**
 * User: cpoopc
 * Date: 2016-05-15
 * Time: 11:36
 * Ver.: 0.1
 */
public class CacheNetOnSubscribeFactory {

    private boolean syncMode;

    public CacheNetOnSubscribeFactory() {
        this.syncMode = true;
    }

    public CacheNetOnSubscribeFactory(boolean syncMode) {
        this.syncMode = syncMode;
    }

    public <T> ObservableOnSubscribe<T> create(Observable<T> cacheObservable, Observable<T> netObservable, Consumer<T> storeCacheAction) {
        if (syncMode) {
            return new SyncOnSubscribeCacheNet<>(cacheObservable, netObservable, storeCacheAction);
        } else {
            return new AsyncOnSubscribeCacheNet<>(cacheObservable, netObservable, storeCacheAction);
        }
    }


}
