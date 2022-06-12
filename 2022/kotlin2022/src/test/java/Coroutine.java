import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import kotlin.coroutines.jvm.internal.ContinuationImpl;

@Metadata(
        mv = {1, 6, 0},
        k = 1,
        xi = 48,
        d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002J\u0019\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0086@ø\u0001\u0000¢\u0006\u0002\u0010\u0007\u0082\u0002\u0004\n\u0002\b\u0019¨\u0006\b"},
        d2 = {"LCoroutine;", "", "()V", "mySuspendFunc", "", "d", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "kotlin2022"}
)
public final class Coroutine {
    @Nullable
    public final Object mySuspendFunc(long d, @NotNull Continuation var3) {
        Object $continuation;
        class MyCoroutineImpl extends ContinuationImpl {
            Object result;
            int label;

            public MyCoroutineImpl(@Nullable Continuation<Object> completion) {
                super(completion);
            }

            @Nullable
            @Override
            protected Object invokeSuspend(@NotNull Object result) {
                this.result = result;
                this.label |= Integer.MIN_VALUE;
                return Coroutine.this.mySuspendFunc(d, (Continuation) this);
            }
        }

        label20:
        {
            if (var3 instanceof MyCoroutineImpl){
            $continuation = ( MyCoroutineImpl)var3;
            if ((((MyCoroutineImpl) $continuation).label & Integer.MIN_VALUE) !=0){
                (( MyCoroutineImpl) $continuation).label -= Integer.MIN_VALUE;
                break label20;
            }
        }

            $continuation = new ContinuationImpl(var3) {
                // $FF: synthetic field
                Object result;
                int label;

                @Nullable
                public final Object invokeSuspend(@NotNull Object $result) {
                    this.result = $result;
                    this.label |= Integer.MIN_VALUE;
                    return Coroutine.this.mySuspendFunc(0L, (Continuation) this);
                }
            };
        }

        Object $result = (( MyCoroutineImpl) $continuation).result;
        Object var7 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        Object var10000;
        switch ((( MyCoroutineImpl) $continuation).label){
            case 0:
                ResultKt.throwOnFailure($result);
                System.out.println("START");
                (( MyCoroutineImpl) $continuation).label = 1;
                var10000 = CoroutineKt.myDelay(d, (Continuation) $continuation);
                if (var10000 == var7) {
                    return var7;
                }
                break;
            case 1:
                ResultKt.throwOnFailure($result);
                var10000 = $result;
                break;
            default:
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
        }

        int x = ((Number) var10000).intValue();
        System.out.println(x);
        return Boxing.boxInt(x);
    }
}
