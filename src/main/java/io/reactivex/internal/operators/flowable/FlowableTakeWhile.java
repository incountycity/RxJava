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

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class FlowableTakeWhile<T> extends Flowable<T> {
    final Publisher<T> source;
    final Predicate<? super T> predicate;
    public FlowableTakeWhile(Publisher<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new TakeWhileSubscriber<T>(s, predicate));
    }
    
    static final class TakeWhileSubscriber<T> implements Subscriber<T>, Subscription {
        final Subscriber<? super T> actual;
        final Predicate<? super T> predicate;
        
        Subscription s;
        
        boolean done;
        
        public TakeWhileSubscriber(Subscriber<? super T> actual, Predicate<? super T> predicate) {
            this.actual = actual;
            this.predicate = predicate;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
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
                s.cancel();
                actual.onError(e);
                return;
            }
            
            if (!b) {
                done = true;
                s.cancel();
                actual.onComplete();
                return;
            }
            
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                return;
            }
            done = true;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
