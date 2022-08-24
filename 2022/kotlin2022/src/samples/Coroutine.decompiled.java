import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.ContinuationImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(
        mv = {1, 6, 0},
        k = 1,
        xi = 48,
        d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002J\u0011\u0010\u0003\u001a\u00020\u0004H\u0086@ø\u0001\u0000¢\u0006\u0002\u0010\u0005\u0082\u0002\u0004\n\u0002\b\u0019¨\u0006\u0006"},
        d2 = {"LCoroutine;", "", "()V", "func0", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "kotlin2022"}
)
public final class Coroutine {
    @Nullable
    public final Object func0(@NotNull Continuation var1) {
        Object $continuation;
        label20: {
            if (var1 instanceof <undefinedtype>) {
                $continuation = (<undefinedtype>)var1;
                if ((((<undefinedtype>)$continuation).label & Integer.MIN_VALUE) != 0) {
                    ((<undefinedtype>)$continuation).label -= Integer.MIN_VALUE;
                    break label20;
                }
            }

            $continuation = new ContinuationImpl(var1) {
                // $FF: synthetic field
                Object result;
                int label;

                @Nullable
                public final Object invokeSuspend(@NotNull Object $result) {
                    this.result = $result;
                    this.label |= Integer.MIN_VALUE;
                    return Coroutine.this.func0((Continuation)this);
                }
            };
        }

        Object $result = ((<undefinedtype>)$continuation).result;
        Object var5 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        Object var10000;
        switch (((<undefinedtype>)$continuation).label) {
            case 0:
                ResultKt.throwOnFailure($result);
                System.out.println("START");
                ((<undefinedtype>)$continuation).label = 1;
                var10000 = CoroutineKt.myDelay(10L, (Continuation)$continuation);
                if (var10000 == var5) {
                    return var5;
                }
                break;
            case 1:
                ResultKt.throwOnFailure($result);
                var10000 = $result;
                break;
            default:
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
        }

        int x = ((Number)var10000).intValue();
        System.out.println(x);
        return Boxing.boxInt(x);
    }
}
