package cloud.apposs.robot.harness.delegate;

import cloud.apposs.react.IoSubscriber;
import cloud.apposs.robot.harness.provider.AIResponse;

public class DelegateWorkerSubscriber extends IoSubscriber<AIResponse> {
    private final IoSubscriber<? super String> subscriber;

    public DelegateWorkerSubscriber(IoSubscriber<? super String> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onNext(AIResponse value) throws Exception {
        subscriber.onNext(value.getContent());
    }

    @Override
    public void onError(Throwable cause) {
        subscriber.onError(cause);
    }
}
