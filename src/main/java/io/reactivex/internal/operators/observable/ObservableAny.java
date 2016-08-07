/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.internal.operators.observable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.disposables.DisposableHelper;

public final class ObservableAny<T> extends Observable<Boolean> {
    final ObservableSource<T> source;
    final Predicate<? super T> predicate;
    public ObservableAny(ObservableSource<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    protected void subscribeActual(Observer<? super Boolean> t) {
        source.subscribe(new AnySubscriber<T>(t, predicate));
    }
    
    static final class AnySubscriber<T> implements Observer<T>, Disposable {
        
        final Observer<? super Boolean> actual;
        final Predicate<? super T> predicate;
        
        Disposable s;
        
        boolean done;

        public AnySubscriber(Observer<? super Boolean> actual, Predicate<? super T> predicate) {
            this.actual = actual;
            this.predicate = predicate;
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                done = true;
                s.dispose();
                actual.onError(e);
                return;
            }
            if (b) {
                done = true;
                s.dispose();
                actual.onNext(true);
                actual.onComplete();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                actual.onError(t);
            }
        }
        
        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                actual.onNext(false);
                actual.onComplete();
            }
        }

        @Override
        public void dispose() {
            s.dispose();
        }
        
        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }
    }
}
