//region block: polyfills
if (typeof Math.imul === 'undefined') {
  Math.imul = function imul(a, b) {
    return (a & 4.29490176E9) * (b & 65535) + (a & 65535) * (b | 0) | 0;
  };
}
if (typeof ArrayBuffer.isView === 'undefined') {
  ArrayBuffer.isView = function (a) {
    return a != null && a.__proto__ != null && a.__proto__.__proto__ === Int8Array.prototype.__proto__;
  };
}
if (typeof Array.prototype.fill === 'undefined') {
  // Polyfill from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/fill#Polyfill
  Object.defineProperty(Array.prototype, 'fill', {value: function (value) {
    // Steps 1-2.
    if (this == null) {
      throw new TypeError('this is null or not defined');
    }
    var O = Object(this); // Steps 3-5.
    var len = O.length >>> 0; // Steps 6-7.
    var start = arguments[1];
    var relativeStart = start >> 0; // Step 8.
    var k = relativeStart < 0 ? Math.max(len + relativeStart, 0) : Math.min(relativeStart, len); // Steps 9-10.
    var end = arguments[2];
    var relativeEnd = end === undefined ? len : end >> 0; // Step 11.
    var finalValue = relativeEnd < 0 ? Math.max(len + relativeEnd, 0) : Math.min(relativeEnd, len); // Step 12.
    while (k < finalValue) {
      O[k] = value;
      k++;
    }
    ; // Step 13.
    return O;
  }});
}
[Int8Array, Int16Array, Uint16Array, Int32Array, Float32Array, Float64Array].forEach(function (TypedArray) {
  if (typeof TypedArray.prototype.fill === 'undefined') {
    Object.defineProperty(TypedArray.prototype, 'fill', {value: Array.prototype.fill});
  }
});
if (typeof Math.clz32 === 'undefined') {
  Math.clz32 = function (log, LN2) {
    return function (x) {
      var asUint = x >>> 0;
      if (asUint === 0) {
        return 32;
      }
      return 31 - (log(asUint) / LN2 | 0) | 0; // the "| 0" acts like math.floor
    };
  }(Math.log, Math.LN2);
}
//endregion
//region block: imports
var imul_0 = Math.imul;
var isView = ArrayBuffer.isView;
var clz32 = Math.clz32;
//endregion
//region block: pre-declaration
initMetadataForInterface(CharSequence, 'CharSequence');
initMetadataForClass(Number_0, 'Number');
initMetadataForClass(Char, 'Char');
initMetadataForInterface(Collection, 'Collection');
initMetadataForInterface(KtList, 'List', VOID, VOID, [Collection]);
initMetadataForInterface(KtSet, 'Set', VOID, VOID, [Collection]);
initMetadataForInterface(Entry, 'Entry');
initMetadataForInterface(KtMap, 'Map');
initMetadataForCompanion(Companion);
initMetadataForClass(Enum, 'Enum');
initMetadataForCompanion(Companion_0);
initMetadataForClass(Long, 'Long', VOID, Number_0);
initMetadataForInterface(FunctionAdapter, 'FunctionAdapter');
initMetadataForInterface(Comparator, 'Comparator');
initMetadataForObject(Unit, 'Unit');
initMetadataForClass(AbstractCollection, 'AbstractCollection', VOID, VOID, [Collection]);
initMetadataForClass(AbstractMutableCollection, 'AbstractMutableCollection', VOID, AbstractCollection, [AbstractCollection, Collection]);
initMetadataForClass(IteratorImpl, 'IteratorImpl');
initMetadataForClass(AbstractMutableList, 'AbstractMutableList', VOID, AbstractMutableCollection, [AbstractMutableCollection, Collection, KtList]);
initMetadataForClass(AbstractMutableSet, 'AbstractMutableSet', VOID, AbstractMutableCollection, [AbstractMutableCollection, Collection, KtSet]);
initMetadataForCompanion(Companion_1);
initMetadataForClass(ArrayList, 'ArrayList', ArrayList_init_$Create$, AbstractMutableList, [AbstractMutableList, Collection, KtList]);
initMetadataForClass(HashSet, 'HashSet', HashSet_init_$Create$, AbstractMutableSet, [AbstractMutableSet, Collection, KtSet]);
initMetadataForCompanion(Companion_2);
initMetadataForClass(Itr, 'Itr');
initMetadataForClass(KeysItr, 'KeysItr', VOID, Itr);
initMetadataForClass(EntriesItr, 'EntriesItr', VOID, Itr);
initMetadataForClass(EntryRef, 'EntryRef', VOID, VOID, [Entry]);
function containsAllEntries(m) {
  var tmp$ret$0;
  $l$block_0: {
    // Inline function 'kotlin.collections.all' call
    var tmp;
    if (isInterface(m, Collection)) {
      tmp = m.g();
    } else {
      tmp = false;
    }
    if (tmp) {
      tmp$ret$0 = true;
      break $l$block_0;
    }
    var _iterator__ex2g4s = m.c();
    while (_iterator__ex2g4s.d()) {
      var element = _iterator__ex2g4s.e();
      // Inline function 'kotlin.js.unsafeCast' call
      // Inline function 'kotlin.js.asDynamic' call
      var entry = element;
      var tmp_0;
      if (!(entry == null) ? isInterface(entry, Entry) : false) {
        tmp_0 = this.p3(entry);
      } else {
        tmp_0 = false;
      }
      if (!tmp_0) {
        tmp$ret$0 = false;
        break $l$block_0;
      }
    }
    tmp$ret$0 = true;
  }
  return tmp$ret$0;
}
initMetadataForInterface(InternalMap, 'InternalMap');
initMetadataForClass(InternalHashMap, 'InternalHashMap', InternalHashMap_init_$Create$, VOID, [InternalMap]);
initMetadataForClass(LinkedHashSet, 'LinkedHashSet', LinkedHashSet_init_$Create$, HashSet, [HashSet, Collection, KtSet]);
initMetadataForClass(BaseOutput, 'BaseOutput');
initMetadataForClass(NodeJsOutput, 'NodeJsOutput', VOID, BaseOutput);
initMetadataForClass(BufferedOutput, 'BufferedOutput', BufferedOutput, BaseOutput);
initMetadataForClass(BufferedOutputToConsoleLog, 'BufferedOutputToConsoleLog', BufferedOutputToConsoleLog, BufferedOutput);
initMetadataForInterface(Continuation, 'Continuation');
initMetadataForClass(InterceptedCoroutine, 'InterceptedCoroutine', VOID, VOID, [Continuation]);
initMetadataForClass(CoroutineImpl, 'CoroutineImpl', VOID, InterceptedCoroutine, [InterceptedCoroutine, Continuation]);
initMetadataForObject(CompletedContinuation, 'CompletedContinuation', VOID, VOID, [Continuation]);
initMetadataForClass(Exception, 'Exception', Exception_init_$Create$, Error);
initMetadataForClass(RuntimeException, 'RuntimeException', RuntimeException_init_$Create$, Exception);
initMetadataForClass(IllegalStateException, 'IllegalStateException', IllegalStateException_init_$Create$, RuntimeException);
initMetadataForClass(CancellationException, 'CancellationException', CancellationException_init_$Create$, IllegalStateException);
initMetadataForClass(createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$1, VOID, VOID, CoroutineImpl);
initMetadataForClass(createSimpleCoroutineForSuspendFunction$1, VOID, VOID, CoroutineImpl);
initMetadataForClass(IllegalArgumentException, 'IllegalArgumentException', IllegalArgumentException_init_$Create$, RuntimeException);
initMetadataForClass(UnsupportedOperationException, 'UnsupportedOperationException', UnsupportedOperationException_init_$Create$, RuntimeException);
initMetadataForClass(NoSuchElementException, 'NoSuchElementException', NoSuchElementException_init_$Create$, RuntimeException);
initMetadataForClass(Error_0, 'Error', Error_init_$Create$, Error);
initMetadataForClass(IndexOutOfBoundsException, 'IndexOutOfBoundsException', IndexOutOfBoundsException_init_$Create$, RuntimeException);
initMetadataForClass(ConcurrentModificationException, 'ConcurrentModificationException', ConcurrentModificationException_init_$Create$, RuntimeException);
initMetadataForClass(NullPointerException, 'NullPointerException', NullPointerException_init_$Create$, RuntimeException);
initMetadataForClass(NoWhenBranchMatchedException, 'NoWhenBranchMatchedException', NoWhenBranchMatchedException_init_$Create$, RuntimeException);
initMetadataForClass(ClassCastException, 'ClassCastException', ClassCastException_init_$Create$, RuntimeException);
initMetadataForClass(UninitializedPropertyAccessException, 'UninitializedPropertyAccessException', UninitializedPropertyAccessException_init_$Create$, RuntimeException);
initMetadataForInterface(KClass, 'KClass');
initMetadataForClass(KClassImpl, 'KClassImpl', VOID, VOID, [KClass]);
initMetadataForObject(NothingKClassImpl, 'NothingKClassImpl', VOID, KClassImpl);
initMetadataForClass(PrimitiveKClassImpl, 'PrimitiveKClassImpl', VOID, KClassImpl);
initMetadataForClass(SimpleKClassImpl, 'SimpleKClassImpl', VOID, KClassImpl);
initMetadataForObject(PrimitiveClasses, 'PrimitiveClasses');
initMetadataForClass(StringBuilder, 'StringBuilder', StringBuilder_init_$Create$_0, VOID, [CharSequence]);
initMetadataForClass(sam$kotlin_Comparator$0, 'sam$kotlin_Comparator$0', VOID, VOID, [Comparator, FunctionAdapter]);
initMetadataForCompanion(Companion_3);
initMetadataForCompanion(Companion_4);
initMetadataForCompanion(Companion_5);
initMetadataForClass(ArrayDeque, 'ArrayDeque', ArrayDeque_init_$Create$, AbstractMutableList);
initMetadataForObject(Key, 'Key');
function plus(context) {
  var tmp;
  if (context === EmptyCoroutineContext_getInstance()) {
    tmp = this;
  } else {
    tmp = context.h7(this, CoroutineContext$plus$lambda);
  }
  return tmp;
}
initMetadataForInterface(CoroutineContext, 'CoroutineContext');
function get(key) {
  var tmp;
  if (equals(this.k(), key)) {
    tmp = isInterface(this, Element) ? this : THROW_CCE();
  } else {
    tmp = null;
  }
  return tmp;
}
function fold(initial, operation) {
  return operation(initial, this);
}
function minusKey(key) {
  return equals(this.k(), key) ? EmptyCoroutineContext_getInstance() : this;
}
initMetadataForInterface(Element, 'Element', VOID, VOID, [CoroutineContext]);
function releaseInterceptedContinuation(continuation) {
}
function get_0(key) {
  if (key instanceof AbstractCoroutineContextKey) {
    var tmp;
    if (key.f7(this.k())) {
      var tmp_0 = key.e7(this);
      tmp = (!(tmp_0 == null) ? isInterface(tmp_0, Element) : false) ? tmp_0 : null;
    } else {
      tmp = null;
    }
    return tmp;
  }
  var tmp_1;
  if (Key_instance === key) {
    tmp_1 = isInterface(this, Element) ? this : THROW_CCE();
  } else {
    tmp_1 = null;
  }
  return tmp_1;
}
function minusKey_0(key) {
  if (key instanceof AbstractCoroutineContextKey) {
    return key.f7(this.k()) && !(key.e7(this) == null) ? EmptyCoroutineContext_getInstance() : this;
  }
  return Key_instance === key ? EmptyCoroutineContext_getInstance() : this;
}
initMetadataForInterface(ContinuationInterceptor, 'ContinuationInterceptor', VOID, VOID, [Element]);
initMetadataForObject(EmptyCoroutineContext, 'EmptyCoroutineContext', VOID, VOID, [CoroutineContext]);
initMetadataForClass(CombinedContext, 'CombinedContext', VOID, VOID, [CoroutineContext]);
initMetadataForClass(AbstractCoroutineContextKey, 'AbstractCoroutineContextKey');
initMetadataForClass(AbstractCoroutineContextElement, 'AbstractCoroutineContextElement', VOID, VOID, [Element]);
initMetadataForClass(CoroutineSingletons, 'CoroutineSingletons', VOID, Enum);
initMetadataForCompanion(Companion_6);
initMetadataForClass(Failure, 'Failure');
initMetadataForClass(NotImplementedError, 'NotImplementedError', NotImplementedError, Error_0);
initMetadataForClass(atomicfu$TraceBase, 'TraceBase');
initMetadataForObject(None, 'None', VOID, atomicfu$TraceBase);
initMetadataForClass(AtomicRef, 'AtomicRef');
initMetadataForClass(AtomicBoolean, 'AtomicBoolean');
initMetadataForClass(AtomicInt, 'AtomicInt');
function invokeOnCompletion$default(onCancelling, invokeImmediately, handler, $super) {
  onCancelling = onCancelling === VOID ? false : onCancelling;
  invokeImmediately = invokeImmediately === VOID ? true : invokeImmediately;
  return $super === VOID ? this.a9(onCancelling, invokeImmediately, handler) : $super.a9.call(this, onCancelling, invokeImmediately, handler);
}
initMetadataForInterface(Job, 'Job', VOID, VOID, [Element], [0]);
initMetadataForInterface(ParentJob, 'ParentJob', VOID, VOID, [Job], [0]);
initMetadataForClass(JobSupport, 'JobSupport', VOID, VOID, [Job, ParentJob], [0]);
initMetadataForInterface(CoroutineScope, 'CoroutineScope');
initMetadataForClass(AbstractCoroutine, 'AbstractCoroutine', VOID, JobSupport, [JobSupport, Job, Continuation, CoroutineScope], [0]);
initMetadataForClass(StandaloneCoroutine, 'StandaloneCoroutine', VOID, AbstractCoroutine, VOID, [0]);
initMetadataForClass(LazyStandaloneCoroutine, 'LazyStandaloneCoroutine', VOID, StandaloneCoroutine, VOID, [0]);
initMetadataForInterface(Runnable, 'Runnable');
initMetadataForClass(SchedulerTask, 'SchedulerTask', VOID, VOID, [Runnable]);
initMetadataForClass(DispatchedTask, 'DispatchedTask', VOID, SchedulerTask);
initMetadataForClass(CancellableContinuationImpl, 'CancellableContinuationImpl', VOID, DispatchedTask, [DispatchedTask, Continuation]);
initMetadataForInterface(NotCompleted, 'NotCompleted');
initMetadataForClass(CancelHandlerBase, 'CancelHandlerBase');
initMetadataForClass(CancelHandler, 'CancelHandler', VOID, CancelHandlerBase, [CancelHandlerBase, NotCompleted]);
initMetadataForObject(Active, 'Active', VOID, VOID, [NotCompleted]);
initMetadataForClass(CompletedContinuation_0, 'CompletedContinuation');
initMetadataForClass(CompletedExceptionally, 'CompletedExceptionally');
initMetadataForClass(CancelledContinuation, 'CancelledContinuation', VOID, CompletedExceptionally);
initMetadataForClass(CompletedWithCancellation, 'CompletedWithCancellation');
initMetadataForObject(Key_0, 'Key', VOID, AbstractCoroutineContextKey);
initMetadataForClass(CoroutineDispatcher, 'CoroutineDispatcher', VOID, AbstractCoroutineContextElement, [AbstractCoroutineContextElement, ContinuationInterceptor]);
initMetadataForObject(Key_1, 'Key');
initMetadataForClass(CoroutineStart, 'CoroutineStart', VOID, Enum);
initMetadataForClass(EventLoop, 'EventLoop', VOID, CoroutineDispatcher);
initMetadataForObject(ThreadLocalEventLoop, 'ThreadLocalEventLoop');
initMetadataForClass(CompletionHandlerException, 'CompletionHandlerException', VOID, RuntimeException);
initMetadataForClass(CoroutinesInternalError, 'CoroutinesInternalError', VOID, Error_0);
initMetadataForObject(Key_2, 'Key');
initMetadataForInterface(ChildHandle, 'ChildHandle');
initMetadataForObject(NonDisposableHandle, 'NonDisposableHandle', VOID, VOID, [ChildHandle]);
initMetadataForInterface(Incomplete, 'Incomplete');
initMetadataForClass(Empty, 'Empty', VOID, VOID, [Incomplete]);
initMetadataForClass(LinkedListNode, 'LinkedListNode', LinkedListNode);
initMetadataForClass(LinkedListHead, 'LinkedListHead', LinkedListHead, LinkedListNode);
initMetadataForClass(NodeList, 'NodeList', NodeList, LinkedListHead, [LinkedListHead, Incomplete]);
initMetadataForClass(CompletionHandlerBase, 'CompletionHandlerBase', VOID, LinkedListNode);
initMetadataForClass(JobNode, 'JobNode', VOID, CompletionHandlerBase, [CompletionHandlerBase, Incomplete]);
initMetadataForClass(SynchronizedObject, 'SynchronizedObject', SynchronizedObject);
initMetadataForClass(Finishing, 'Finishing', VOID, SynchronizedObject, [SynchronizedObject, Incomplete]);
initMetadataForClass(ChildCompletion, 'ChildCompletion', VOID, JobNode);
initMetadataForClass(JobCancellingNode, 'JobCancellingNode', VOID, JobNode);
initMetadataForClass(InactiveNodeList, 'InactiveNodeList', VOID, VOID, [Incomplete]);
initMetadataForClass(ChildHandleNode, 'ChildHandleNode', VOID, JobCancellingNode, [JobCancellingNode, ChildHandle]);
initMetadataForClass(InvokeOnCancelling, 'InvokeOnCancelling', VOID, JobCancellingNode);
initMetadataForClass(InvokeOnCompletion, 'InvokeOnCompletion', VOID, JobNode);
initMetadataForClass(IncompleteStateBox, 'IncompleteStateBox');
initMetadataForClass(ChildContinuation, 'ChildContinuation', VOID, JobCancellingNode);
initMetadataForClass(JobImpl, 'JobImpl', VOID, JobSupport, [JobSupport, Job], [0]);
initMetadataForClass(MainCoroutineDispatcher, 'MainCoroutineDispatcher', VOID, CoroutineDispatcher);
initMetadataForClass(SupervisorJobImpl, 'SupervisorJobImpl', VOID, JobImpl, VOID, [0]);
initMetadataForClass(TimeoutCancellationException, 'TimeoutCancellationException', VOID, CancellationException);
initMetadataForObject(Unconfined, 'Unconfined', VOID, CoroutineDispatcher);
initMetadataForObject(Key_3, 'Key');
initMetadataForClass(OpDescriptor, 'OpDescriptor');
initMetadataForClass(ConcurrentLinkedListNode, 'ConcurrentLinkedListNode');
initMetadataForClass(Segment, 'Segment', VOID, ConcurrentLinkedListNode, [ConcurrentLinkedListNode, NotCompleted]);
initMetadataForObject(ExceptionSuccessfullyProcessed, 'ExceptionSuccessfullyProcessed', VOID, Exception);
initMetadataForClass(DispatchedContinuation, 'DispatchedContinuation', VOID, DispatchedTask, [DispatchedTask, Continuation]);
initMetadataForClass(ContextScope, 'ContextScope', VOID, VOID, [CoroutineScope]);
initMetadataForClass(Symbol_0, 'Symbol');
initMetadataForClass(SetTimeoutBasedDispatcher, 'SetTimeoutBasedDispatcher', VOID, CoroutineDispatcher, VOID, [1]);
initMetadataForObject(NodeDispatcher, 'NodeDispatcher', VOID, SetTimeoutBasedDispatcher, VOID, [1]);
initMetadataForClass(MessageQueue, 'MessageQueue', VOID, VOID, [Collection, KtList]);
initMetadataForClass(ScheduledMessageQueue, 'ScheduledMessageQueue', VOID, MessageQueue);
initMetadataForClass(WindowMessageQueue, 'WindowMessageQueue', VOID, MessageQueue);
initMetadataForObject(Dispatchers, 'Dispatchers');
initMetadataForClass(JsMainDispatcher, 'JsMainDispatcher', VOID, MainCoroutineDispatcher);
initMetadataForClass(UnconfinedEventLoop, 'UnconfinedEventLoop', UnconfinedEventLoop, EventLoop);
initMetadataForClass(JobCancellationException, 'JobCancellationException', VOID, CancellationException);
initMetadataForObject(TaskContext, 'TaskContext');
initMetadataForClass(DiagnosticCoroutineContextException, 'DiagnosticCoroutineContextException', VOID, RuntimeException);
initMetadataForObject(SetTimeoutDispatcher, 'SetTimeoutDispatcher', VOID, SetTimeoutBasedDispatcher, VOID, [1]);
initMetadataForClass(WindowDispatcher, 'WindowDispatcher', VOID, CoroutineDispatcher, VOID, [1]);
initMetadataForClass(CommonThreadLocal, 'CommonThreadLocal', CommonThreadLocal);
initMetadataForLambda(WebRTCController$slambda, CoroutineImpl, VOID, [1]);
initMetadataForLambda(WebRTCController$startListening$lambda$slambda, CoroutineImpl, VOID, [1]);
initMetadataForCoroutine($setupP2PCOROUTINE$, CoroutineImpl);
initMetadataForClass(WebRTCController, 'WebRTCController', VOID, VOID, VOID, [0]);
//endregion
function CharSequence() {
}
function Number_0() {
}
function indexOf(_this__u8e3s4, element) {
  if (element == null) {
    var inductionVariable = 0;
    var last = _this__u8e3s4.length - 1 | 0;
    if (inductionVariable <= last)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (_this__u8e3s4[index] == null) {
          return index;
        }
      }
       while (inductionVariable <= last);
  } else {
    var inductionVariable_0 = 0;
    var last_0 = _this__u8e3s4.length - 1 | 0;
    if (inductionVariable_0 <= last_0)
      do {
        var index_0 = inductionVariable_0;
        inductionVariable_0 = inductionVariable_0 + 1 | 0;
        if (equals(element, _this__u8e3s4[index_0])) {
          return index_0;
        }
      }
       while (inductionVariable_0 <= last_0);
  }
  return -1;
}
function get_lastIndex(_this__u8e3s4) {
  return _this__u8e3s4.length - 1 | 0;
}
function joinToString(_this__u8e3s4, separator, prefix, postfix, limit, truncated, transform) {
  separator = separator === VOID ? ', ' : separator;
  prefix = prefix === VOID ? '' : prefix;
  postfix = postfix === VOID ? '' : postfix;
  limit = limit === VOID ? -1 : limit;
  truncated = truncated === VOID ? '...' : truncated;
  transform = transform === VOID ? null : transform;
  return joinTo(_this__u8e3s4, StringBuilder_init_$Create$_0(), separator, prefix, postfix, limit, truncated, transform).toString();
}
function joinTo(_this__u8e3s4, buffer, separator, prefix, postfix, limit, truncated, transform) {
  separator = separator === VOID ? ', ' : separator;
  prefix = prefix === VOID ? '' : prefix;
  postfix = postfix === VOID ? '' : postfix;
  limit = limit === VOID ? -1 : limit;
  truncated = truncated === VOID ? '...' : truncated;
  transform = transform === VOID ? null : transform;
  buffer.b(prefix);
  var count = 0;
  var inductionVariable = 0;
  var last = _this__u8e3s4.length;
  $l$loop: while (inductionVariable < last) {
    var element = _this__u8e3s4[inductionVariable];
    inductionVariable = inductionVariable + 1 | 0;
    count = count + 1 | 0;
    if (count > 1) {
      buffer.b(separator);
    }
    if (limit < 0 || count <= limit) {
      appendElement(buffer, element, transform);
    } else
      break $l$loop;
  }
  if (limit >= 0 && count > limit) {
    buffer.b(truncated);
  }
  buffer.b(postfix);
  return buffer;
}
function joinToString_0(_this__u8e3s4, separator, prefix, postfix, limit, truncated, transform) {
  separator = separator === VOID ? ', ' : separator;
  prefix = prefix === VOID ? '' : prefix;
  postfix = postfix === VOID ? '' : postfix;
  limit = limit === VOID ? -1 : limit;
  truncated = truncated === VOID ? '...' : truncated;
  transform = transform === VOID ? null : transform;
  return joinTo_0(_this__u8e3s4, StringBuilder_init_$Create$_0(), separator, prefix, postfix, limit, truncated, transform).toString();
}
function joinTo_0(_this__u8e3s4, buffer, separator, prefix, postfix, limit, truncated, transform) {
  separator = separator === VOID ? ', ' : separator;
  prefix = prefix === VOID ? '' : prefix;
  postfix = postfix === VOID ? '' : postfix;
  limit = limit === VOID ? -1 : limit;
  truncated = truncated === VOID ? '...' : truncated;
  transform = transform === VOID ? null : transform;
  buffer.b(prefix);
  var count = 0;
  var _iterator__ex2g4s = _this__u8e3s4.c();
  $l$loop: while (_iterator__ex2g4s.d()) {
    var element = _iterator__ex2g4s.e();
    count = count + 1 | 0;
    if (count > 1) {
      buffer.b(separator);
    }
    if (limit < 0 || count <= limit) {
      appendElement(buffer, element, transform);
    } else
      break $l$loop;
  }
  if (limit >= 0 && count > limit) {
    buffer.b(truncated);
  }
  buffer.b(postfix);
  return buffer;
}
function coerceAtLeast(_this__u8e3s4, minimumValue) {
  return _this__u8e3s4 < minimumValue ? minimumValue : _this__u8e3s4;
}
function coerceAtMost(_this__u8e3s4, maximumValue) {
  return _this__u8e3s4 > maximumValue ? maximumValue : _this__u8e3s4;
}
function _Char___init__impl__6a9atx(value) {
  return value;
}
function _get_value__a43j40($this) {
  return $this;
}
function Char__compareTo_impl_ypi4mb($this, other) {
  return _get_value__a43j40($this) - _get_value__a43j40(other) | 0;
}
function toString($this) {
  // Inline function 'kotlin.js.unsafeCast' call
  return String.fromCharCode(_get_value__a43j40($this));
}
function Char() {
}
function KtList() {
}
function Collection() {
}
function KtSet() {
}
function Entry() {
}
function KtMap() {
}
function Companion() {
}
var Companion_instance;
function Companion_getInstance() {
  return Companion_instance;
}
function Enum(name, ordinal) {
  this.n_1 = name;
  this.o_1 = ordinal;
}
protoOf(Enum).p = function (other) {
  return compareTo(this.o_1, other.o_1);
};
protoOf(Enum).q = function (other) {
  return this.p(other instanceof Enum ? other : THROW_CCE());
};
protoOf(Enum).equals = function (other) {
  return this === other;
};
protoOf(Enum).hashCode = function () {
  return identityHashCode(this);
};
protoOf(Enum).toString = function () {
  return this.n_1;
};
function toString_0(_this__u8e3s4) {
  var tmp1_elvis_lhs = _this__u8e3s4 == null ? null : toString_1(_this__u8e3s4);
  return tmp1_elvis_lhs == null ? 'null' : tmp1_elvis_lhs;
}
function Companion_0() {
  Companion_instance_0 = this;
  this.r_1 = new Long(0, -2147483648);
  this.s_1 = new Long(-1, 2147483647);
  this.t_1 = 8;
  this.u_1 = 64;
}
var Companion_instance_0;
function Companion_getInstance_0() {
  if (Companion_instance_0 == null)
    new Companion_0();
  return Companion_instance_0;
}
function Long(low, high) {
  Companion_getInstance_0();
  Number_0.call(this);
  this.v_1 = low;
  this.w_1 = high;
}
protoOf(Long).x = function (other) {
  return compare(this, other);
};
protoOf(Long).q = function (other) {
  return this.x(other instanceof Long ? other : THROW_CCE());
};
protoOf(Long).toString = function () {
  return toStringImpl(this, 10);
};
protoOf(Long).equals = function (other) {
  var tmp;
  if (other instanceof Long) {
    tmp = equalsLong(this, other);
  } else {
    tmp = false;
  }
  return tmp;
};
protoOf(Long).hashCode = function () {
  return hashCode(this);
};
protoOf(Long).valueOf = function () {
  return toNumber(this);
};
function abs(_this__u8e3s4) {
  var tmp;
  // Inline function 'kotlin.js.internal.isNegative' call
  if (_this__u8e3s4 < 0) {
    // Inline function 'kotlin.js.internal.unaryMinus' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp = -_this__u8e3s4;
  } else {
    tmp = _this__u8e3s4;
  }
  return tmp;
}
function implement(interfaces) {
  var maxSize = 1;
  var masks = [];
  var inductionVariable = 0;
  var last = interfaces.length;
  while (inductionVariable < last) {
    var i = interfaces[inductionVariable];
    inductionVariable = inductionVariable + 1 | 0;
    var currentSize = maxSize;
    var tmp0_elvis_lhs = i.prototype.$imask$;
    var imask = tmp0_elvis_lhs == null ? i.$imask$ : tmp0_elvis_lhs;
    if (!(imask == null)) {
      masks.push(imask);
      currentSize = imask.length;
    }
    var iid = i.$metadata$.iid;
    var tmp;
    if (iid == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.let' call
      tmp = bitMaskWith(iid);
    }
    var iidImask = tmp;
    if (!(iidImask == null)) {
      masks.push(iidImask);
      currentSize = Math.max(currentSize, iidImask.length);
    }
    if (currentSize > maxSize) {
      maxSize = currentSize;
    }
  }
  return compositeBitMask(maxSize, masks);
}
function bitMaskWith(activeBit) {
  var numberIndex = activeBit >> 5;
  var intArray = new Int32Array(numberIndex + 1 | 0);
  var positionInNumber = activeBit & 31;
  var numberWithSettledBit = 1 << positionInNumber;
  intArray[numberIndex] = intArray[numberIndex] | numberWithSettledBit;
  return intArray;
}
function compositeBitMask(capacity, masks) {
  var tmp = 0;
  var tmp_0 = new Int32Array(capacity);
  while (tmp < capacity) {
    var tmp_1 = tmp;
    var result = 0;
    var inductionVariable = 0;
    var last = masks.length;
    while (inductionVariable < last) {
      var mask = masks[inductionVariable];
      inductionVariable = inductionVariable + 1 | 0;
      if (tmp_1 < mask.length) {
        result = result | mask[tmp_1];
      }
    }
    tmp_0[tmp_1] = result;
    tmp = tmp + 1 | 0;
  }
  return tmp_0;
}
function isBitSet(_this__u8e3s4, possibleActiveBit) {
  var numberIndex = possibleActiveBit >> 5;
  if (numberIndex > _this__u8e3s4.length)
    return false;
  var positionInNumber = possibleActiveBit & 31;
  var numberWithSettledBit = 1 << positionInNumber;
  return !((_this__u8e3s4[numberIndex] & numberWithSettledBit) === 0);
}
function FunctionAdapter() {
}
function get_buf() {
  _init_properties_bitUtils_kt__nfcg4k();
  return buf;
}
var buf;
function get_bufFloat64() {
  _init_properties_bitUtils_kt__nfcg4k();
  return bufFloat64;
}
var bufFloat64;
var bufFloat32;
function get_bufInt32() {
  _init_properties_bitUtils_kt__nfcg4k();
  return bufInt32;
}
var bufInt32;
function get_lowIndex() {
  _init_properties_bitUtils_kt__nfcg4k();
  return lowIndex;
}
var lowIndex;
function get_highIndex() {
  _init_properties_bitUtils_kt__nfcg4k();
  return highIndex;
}
var highIndex;
function getNumberHashCode(obj) {
  _init_properties_bitUtils_kt__nfcg4k();
  // Inline function 'kotlin.js.jsBitwiseOr' call
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  if ((obj | 0) === obj) {
    return numberToInt(obj);
  }
  get_bufFloat64()[0] = obj;
  return imul_0(get_bufInt32()[get_highIndex()], 31) + get_bufInt32()[get_lowIndex()] | 0;
}
var properties_initialized_bitUtils_kt_i2bo3e;
function _init_properties_bitUtils_kt__nfcg4k() {
  if (!properties_initialized_bitUtils_kt_i2bo3e) {
    properties_initialized_bitUtils_kt_i2bo3e = true;
    buf = new ArrayBuffer(8);
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    bufFloat64 = new Float64Array(get_buf());
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    bufFloat32 = new Float32Array(get_buf());
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    bufInt32 = new Int32Array(get_buf());
    // Inline function 'kotlin.run' call
    get_bufFloat64()[0] = -1.0;
    lowIndex = !(get_bufInt32()[0] === 0) ? 1 : 0;
    highIndex = 1 - get_lowIndex() | 0;
  }
}
function get_ZERO() {
  _init_properties_boxedLong_kt__v24qrw();
  return ZERO;
}
var ZERO;
function get_ONE() {
  _init_properties_boxedLong_kt__v24qrw();
  return ONE;
}
var ONE;
function get_NEG_ONE() {
  _init_properties_boxedLong_kt__v24qrw();
  return NEG_ONE;
}
var NEG_ONE;
function get_MAX_VALUE() {
  _init_properties_boxedLong_kt__v24qrw();
  return MAX_VALUE;
}
var MAX_VALUE;
function get_MIN_VALUE() {
  _init_properties_boxedLong_kt__v24qrw();
  return MIN_VALUE;
}
var MIN_VALUE;
function get_TWO_PWR_24_() {
  _init_properties_boxedLong_kt__v24qrw();
  return TWO_PWR_24_;
}
var TWO_PWR_24_;
function compare(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  if (equalsLong(_this__u8e3s4, other)) {
    return 0;
  }
  var thisNeg = isNegative(_this__u8e3s4);
  var otherNeg = isNegative(other);
  return thisNeg && !otherNeg ? -1 : !thisNeg && otherNeg ? 1 : isNegative(subtract(_this__u8e3s4, other)) ? -1 : 1;
}
function add(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  var a48 = _this__u8e3s4.w_1 >>> 16 | 0;
  var a32 = _this__u8e3s4.w_1 & 65535;
  var a16 = _this__u8e3s4.v_1 >>> 16 | 0;
  var a00 = _this__u8e3s4.v_1 & 65535;
  var b48 = other.w_1 >>> 16 | 0;
  var b32 = other.w_1 & 65535;
  var b16 = other.v_1 >>> 16 | 0;
  var b00 = other.v_1 & 65535;
  var c48 = 0;
  var c32 = 0;
  var c16 = 0;
  var c00 = 0;
  c00 = c00 + (a00 + b00 | 0) | 0;
  c16 = c16 + (c00 >>> 16 | 0) | 0;
  c00 = c00 & 65535;
  c16 = c16 + (a16 + b16 | 0) | 0;
  c32 = c32 + (c16 >>> 16 | 0) | 0;
  c16 = c16 & 65535;
  c32 = c32 + (a32 + b32 | 0) | 0;
  c48 = c48 + (c32 >>> 16 | 0) | 0;
  c32 = c32 & 65535;
  c48 = c48 + (a48 + b48 | 0) | 0;
  c48 = c48 & 65535;
  return new Long(c16 << 16 | c00, c48 << 16 | c32);
}
function subtract(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  return add(_this__u8e3s4, negate(other));
}
function multiply(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  if (isZero(_this__u8e3s4)) {
    return get_ZERO();
  } else if (isZero(other)) {
    return get_ZERO();
  }
  if (equalsLong(_this__u8e3s4, get_MIN_VALUE())) {
    return isOdd(other) ? get_MIN_VALUE() : get_ZERO();
  } else if (equalsLong(other, get_MIN_VALUE())) {
    return isOdd(_this__u8e3s4) ? get_MIN_VALUE() : get_ZERO();
  }
  if (isNegative(_this__u8e3s4)) {
    var tmp;
    if (isNegative(other)) {
      tmp = multiply(negate(_this__u8e3s4), negate(other));
    } else {
      tmp = negate(multiply(negate(_this__u8e3s4), other));
    }
    return tmp;
  } else if (isNegative(other)) {
    return negate(multiply(_this__u8e3s4, negate(other)));
  }
  if (lessThan(_this__u8e3s4, get_TWO_PWR_24_()) && lessThan(other, get_TWO_PWR_24_())) {
    return fromNumber(toNumber(_this__u8e3s4) * toNumber(other));
  }
  var a48 = _this__u8e3s4.w_1 >>> 16 | 0;
  var a32 = _this__u8e3s4.w_1 & 65535;
  var a16 = _this__u8e3s4.v_1 >>> 16 | 0;
  var a00 = _this__u8e3s4.v_1 & 65535;
  var b48 = other.w_1 >>> 16 | 0;
  var b32 = other.w_1 & 65535;
  var b16 = other.v_1 >>> 16 | 0;
  var b00 = other.v_1 & 65535;
  var c48 = 0;
  var c32 = 0;
  var c16 = 0;
  var c00 = 0;
  c00 = c00 + imul_0(a00, b00) | 0;
  c16 = c16 + (c00 >>> 16 | 0) | 0;
  c00 = c00 & 65535;
  c16 = c16 + imul_0(a16, b00) | 0;
  c32 = c32 + (c16 >>> 16 | 0) | 0;
  c16 = c16 & 65535;
  c16 = c16 + imul_0(a00, b16) | 0;
  c32 = c32 + (c16 >>> 16 | 0) | 0;
  c16 = c16 & 65535;
  c32 = c32 + imul_0(a32, b00) | 0;
  c48 = c48 + (c32 >>> 16 | 0) | 0;
  c32 = c32 & 65535;
  c32 = c32 + imul_0(a16, b16) | 0;
  c48 = c48 + (c32 >>> 16 | 0) | 0;
  c32 = c32 & 65535;
  c32 = c32 + imul_0(a00, b32) | 0;
  c48 = c48 + (c32 >>> 16 | 0) | 0;
  c32 = c32 & 65535;
  c48 = c48 + (((imul_0(a48, b00) + imul_0(a32, b16) | 0) + imul_0(a16, b32) | 0) + imul_0(a00, b48) | 0) | 0;
  c48 = c48 & 65535;
  return new Long(c16 << 16 | c00, c48 << 16 | c32);
}
function divide(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  if (isZero(other)) {
    throw Exception_init_$Create$_0('division by zero');
  } else if (isZero(_this__u8e3s4)) {
    return get_ZERO();
  }
  if (equalsLong(_this__u8e3s4, get_MIN_VALUE())) {
    if (equalsLong(other, get_ONE()) || equalsLong(other, get_NEG_ONE())) {
      return get_MIN_VALUE();
    } else if (equalsLong(other, get_MIN_VALUE())) {
      return get_ONE();
    } else {
      var halfThis = shiftRight(_this__u8e3s4, 1);
      var approx = shiftLeft(divide(halfThis, other), 1);
      if (equalsLong(approx, get_ZERO())) {
        return isNegative(other) ? get_ONE() : get_NEG_ONE();
      } else {
        var rem = subtract(_this__u8e3s4, multiply(other, approx));
        return add(approx, divide(rem, other));
      }
    }
  } else if (equalsLong(other, get_MIN_VALUE())) {
    return get_ZERO();
  }
  if (isNegative(_this__u8e3s4)) {
    var tmp;
    if (isNegative(other)) {
      tmp = divide(negate(_this__u8e3s4), negate(other));
    } else {
      tmp = negate(divide(negate(_this__u8e3s4), other));
    }
    return tmp;
  } else if (isNegative(other)) {
    return negate(divide(_this__u8e3s4, negate(other)));
  }
  var res = get_ZERO();
  var rem_0 = _this__u8e3s4;
  while (greaterThanOrEqual(rem_0, other)) {
    var approxDouble = toNumber(rem_0) / toNumber(other);
    var approx2 = Math.max(1.0, Math.floor(approxDouble));
    var log2 = Math.ceil(Math.log(approx2) / Math.LN2);
    var delta = log2 <= 48 ? 1.0 : Math.pow(2.0, log2 - 48);
    var approxRes = fromNumber(approx2);
    var approxRem = multiply(approxRes, other);
    while (isNegative(approxRem) || greaterThan(approxRem, rem_0)) {
      approx2 = approx2 - delta;
      approxRes = fromNumber(approx2);
      approxRem = multiply(approxRes, other);
    }
    if (isZero(approxRes)) {
      approxRes = get_ONE();
    }
    res = add(res, approxRes);
    rem_0 = subtract(rem_0, approxRem);
  }
  return res;
}
function shiftLeft(_this__u8e3s4, numBits) {
  _init_properties_boxedLong_kt__v24qrw();
  var numBits_0 = numBits & 63;
  if (numBits_0 === 0) {
    return _this__u8e3s4;
  } else {
    if (numBits_0 < 32) {
      return new Long(_this__u8e3s4.v_1 << numBits_0, _this__u8e3s4.w_1 << numBits_0 | (_this__u8e3s4.v_1 >>> (32 - numBits_0 | 0) | 0));
    } else {
      return new Long(0, _this__u8e3s4.v_1 << (numBits_0 - 32 | 0));
    }
  }
}
function shiftRight(_this__u8e3s4, numBits) {
  _init_properties_boxedLong_kt__v24qrw();
  var numBits_0 = numBits & 63;
  if (numBits_0 === 0) {
    return _this__u8e3s4;
  } else {
    if (numBits_0 < 32) {
      return new Long(_this__u8e3s4.v_1 >>> numBits_0 | 0 | _this__u8e3s4.w_1 << (32 - numBits_0 | 0), _this__u8e3s4.w_1 >> numBits_0);
    } else {
      return new Long(_this__u8e3s4.w_1 >> (numBits_0 - 32 | 0), _this__u8e3s4.w_1 >= 0 ? 0 : -1);
    }
  }
}
function invert(_this__u8e3s4) {
  _init_properties_boxedLong_kt__v24qrw();
  return new Long(~_this__u8e3s4.v_1, ~_this__u8e3s4.w_1);
}
function convertToInt(_this__u8e3s4) {
  _init_properties_boxedLong_kt__v24qrw();
  return _this__u8e3s4.v_1;
}
function toNumber(_this__u8e3s4) {
  _init_properties_boxedLong_kt__v24qrw();
  return _this__u8e3s4.w_1 * 4.294967296E9 + getLowBitsUnsigned(_this__u8e3s4);
}
function toStringImpl(_this__u8e3s4, radix) {
  _init_properties_boxedLong_kt__v24qrw();
  if (isZero(_this__u8e3s4)) {
    return '0';
  }
  if (isNegative(_this__u8e3s4)) {
    if (equalsLong(_this__u8e3s4, get_MIN_VALUE())) {
      var radixLong = fromInt(radix);
      var div = divide(_this__u8e3s4, radixLong);
      var rem = convertToInt(subtract(multiply(div, radixLong), _this__u8e3s4));
      var tmp = toStringImpl(div, radix);
      // Inline function 'kotlin.js.asDynamic' call
      // Inline function 'kotlin.js.unsafeCast' call
      return tmp + rem.toString(radix);
    } else {
      return '-' + toStringImpl(negate(_this__u8e3s4), radix);
    }
  }
  var digitsPerTime = radix === 2 ? 31 : radix <= 10 ? 9 : radix <= 21 ? 7 : radix <= 35 ? 6 : 5;
  var radixToPower = fromNumber(Math.pow(radix, digitsPerTime));
  var rem_0 = _this__u8e3s4;
  var result = '';
  while (true) {
    var remDiv = divide(rem_0, radixToPower);
    var intval = convertToInt(subtract(rem_0, multiply(remDiv, radixToPower)));
    // Inline function 'kotlin.js.asDynamic' call
    // Inline function 'kotlin.js.unsafeCast' call
    var digits = intval.toString(radix);
    rem_0 = remDiv;
    if (isZero(rem_0)) {
      return digits + result;
    } else {
      while (digits.length < digitsPerTime) {
        digits = '0' + digits;
      }
      result = digits + result;
    }
  }
}
function equalsLong(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  return _this__u8e3s4.w_1 === other.w_1 && _this__u8e3s4.v_1 === other.v_1;
}
function hashCode(l) {
  _init_properties_boxedLong_kt__v24qrw();
  return l.v_1 ^ l.w_1;
}
function fromInt(value) {
  _init_properties_boxedLong_kt__v24qrw();
  return new Long(value, value < 0 ? -1 : 0);
}
function isNegative(_this__u8e3s4) {
  _init_properties_boxedLong_kt__v24qrw();
  return _this__u8e3s4.w_1 < 0;
}
function isZero(_this__u8e3s4) {
  _init_properties_boxedLong_kt__v24qrw();
  return _this__u8e3s4.w_1 === 0 && _this__u8e3s4.v_1 === 0;
}
function isOdd(_this__u8e3s4) {
  _init_properties_boxedLong_kt__v24qrw();
  return (_this__u8e3s4.v_1 & 1) === 1;
}
function negate(_this__u8e3s4) {
  _init_properties_boxedLong_kt__v24qrw();
  return add(invert(_this__u8e3s4), new Long(1, 0));
}
function lessThan(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  return compare(_this__u8e3s4, other) < 0;
}
function fromNumber(value) {
  _init_properties_boxedLong_kt__v24qrw();
  if (isNaN_0(value)) {
    return get_ZERO();
  } else if (value <= -9.223372036854776E18) {
    return get_MIN_VALUE();
  } else if (value + 1 >= 9.223372036854776E18) {
    return get_MAX_VALUE();
  } else if (value < 0) {
    return negate(fromNumber(-value));
  } else {
    var twoPwr32 = 4.294967296E9;
    // Inline function 'kotlin.js.jsBitwiseOr' call
    var tmp = value % twoPwr32 | 0;
    // Inline function 'kotlin.js.jsBitwiseOr' call
    var tmp$ret$1 = value / twoPwr32 | 0;
    return new Long(tmp, tmp$ret$1);
  }
}
function greaterThan(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  return compare(_this__u8e3s4, other) > 0;
}
function greaterThanOrEqual(_this__u8e3s4, other) {
  _init_properties_boxedLong_kt__v24qrw();
  return compare(_this__u8e3s4, other) >= 0;
}
function getLowBitsUnsigned(_this__u8e3s4) {
  _init_properties_boxedLong_kt__v24qrw();
  return _this__u8e3s4.v_1 >= 0 ? _this__u8e3s4.v_1 : 4.294967296E9 + _this__u8e3s4.v_1;
}
var properties_initialized_boxedLong_kt_lfwt2;
function _init_properties_boxedLong_kt__v24qrw() {
  if (!properties_initialized_boxedLong_kt_lfwt2) {
    properties_initialized_boxedLong_kt_lfwt2 = true;
    ZERO = fromInt(0);
    ONE = fromInt(1);
    NEG_ONE = fromInt(-1);
    MAX_VALUE = new Long(-1, 2147483647);
    MIN_VALUE = new Long(0, -2147483648);
    TWO_PWR_24_ = fromInt(16777216);
  }
}
function isString(a) {
  return typeof a === 'string';
}
function charCodeAt(_this__u8e3s4, index) {
  // Inline function 'kotlin.js.asDynamic' call
  return _this__u8e3s4.charCodeAt(index);
}
function charSequenceLength(a) {
  var tmp;
  if (isString(a)) {
    // Inline function 'kotlin.js.asDynamic' call
    // Inline function 'kotlin.js.unsafeCast' call
    tmp = a.length;
  } else {
    tmp = a.a();
  }
  return tmp;
}
function arrayToString(array) {
  return joinToString(array, ', ', '[', ']', VOID, VOID, arrayToString$lambda);
}
function arrayToString$lambda(it) {
  return toString_1(it);
}
function compareTo(a, b) {
  var tmp;
  switch (typeof a) {
    case 'number':
      var tmp_0;
      if (typeof b === 'number') {
        tmp_0 = doubleCompareTo(a, b);
      } else {
        if (b instanceof Long) {
          tmp_0 = doubleCompareTo(a, toNumber(b));
        } else {
          tmp_0 = primitiveCompareTo(a, b);
        }
      }

      tmp = tmp_0;
      break;
    case 'string':
    case 'boolean':
    case 'bigint':
      tmp = primitiveCompareTo(a, b);
      break;
    default:
      tmp = compareToDoNotIntrinsicify(a, b);
      break;
  }
  return tmp;
}
function doubleCompareTo(a, b) {
  var tmp;
  if (a < b) {
    tmp = -1;
  } else if (a > b) {
    tmp = 1;
  } else if (a === b) {
    var tmp_0;
    if (a !== 0) {
      tmp_0 = 0;
    } else {
      // Inline function 'kotlin.js.asDynamic' call
      var ia = 1 / a;
      var tmp_1;
      // Inline function 'kotlin.js.asDynamic' call
      if (ia === 1 / b) {
        tmp_1 = 0;
      } else {
        if (ia < 0) {
          tmp_1 = -1;
        } else {
          tmp_1 = 1;
        }
      }
      tmp_0 = tmp_1;
    }
    tmp = tmp_0;
  } else if (a !== a) {
    tmp = b !== b ? 0 : 1;
  } else {
    tmp = -1;
  }
  return tmp;
}
function primitiveCompareTo(a, b) {
  return a < b ? -1 : a > b ? 1 : 0;
}
function compareToDoNotIntrinsicify(a, b) {
  return a.q(b);
}
function identityHashCode(obj) {
  return getObjectHashCode(obj);
}
function getObjectHashCode(obj) {
  // Inline function 'kotlin.js.jsIn' call
  if (!('kotlinHashCodeValue$' in obj)) {
    var hash = calculateRandomHash();
    var descriptor = new Object();
    descriptor.value = hash;
    descriptor.enumerable = false;
    Object.defineProperty(obj, 'kotlinHashCodeValue$', descriptor);
  }
  // Inline function 'kotlin.js.unsafeCast' call
  return obj['kotlinHashCodeValue$'];
}
function calculateRandomHash() {
  // Inline function 'kotlin.js.jsBitwiseOr' call
  return Math.random() * 4.294967296E9 | 0;
}
function objectCreate(proto) {
  proto = proto === VOID ? null : proto;
  return Object.create(proto);
}
function defineProp(obj, name, getter, setter, enumerable) {
  return Object.defineProperty(obj, name, {configurable: true, get: getter, set: setter, enumerable: enumerable});
}
function toString_1(o) {
  var tmp;
  if (o == null) {
    tmp = 'null';
  } else if (isArrayish(o)) {
    tmp = '[...]';
  } else if (!(typeof o.toString === 'function')) {
    tmp = anyToString(o);
  } else {
    // Inline function 'kotlin.js.unsafeCast' call
    tmp = o.toString();
  }
  return tmp;
}
function anyToString(o) {
  return Object.prototype.toString.call(o);
}
function hashCode_0(obj) {
  if (obj == null)
    return 0;
  var typeOf = typeof obj;
  var tmp;
  switch (typeOf) {
    case 'object':
      tmp = 'function' === typeof obj.hashCode ? obj.hashCode() : getObjectHashCode(obj);
      break;
    case 'function':
      tmp = getObjectHashCode(obj);
      break;
    case 'number':
      tmp = getNumberHashCode(obj);
      break;
    case 'boolean':
      // Inline function 'kotlin.js.unsafeCast' call

      tmp = getBooleanHashCode(obj);
      break;
    case 'string':
      tmp = getStringHashCode(String(obj));
      break;
    case 'bigint':
      // Inline function 'kotlin.js.unsafeCast' call

      tmp = getBigIntHashCode(obj);
      break;
    case 'symbol':
      tmp = getSymbolHashCode(obj);
      break;
    default:
      tmp = function () {
        throw new Error('Unexpected typeof `' + typeOf + '`');
      }();
      break;
  }
  return tmp;
}
function getBooleanHashCode(value) {
  return value ? 1231 : 1237;
}
function getStringHashCode(str) {
  var hash = 0;
  var length = str.length;
  var inductionVariable = 0;
  var last = length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.js.asDynamic' call
      var code = str.charCodeAt(i);
      hash = imul_0(hash, 31) + code | 0;
    }
     while (!(i === last));
  return hash;
}
function getBigIntHashCode(value) {
  var shiftNumber = BigInt(32);
  var mask = BigInt(4.294967295E9);
  var bigNumber = abs(value);
  var hashCode = 0;
  var tmp;
  // Inline function 'kotlin.js.internal.isNegative' call
  if (value < 0) {
    tmp = -1;
  } else {
    tmp = 1;
  }
  var signum = tmp;
  $l$loop: while (true) {
    // Inline function 'kotlin.js.internal.isZero' call
    if (!!(bigNumber == 0)) {
      break $l$loop;
    }
    // Inline function 'kotlin.js.internal.and' call
    // Inline function 'kotlin.js.jsBitwiseAnd' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    // Inline function 'kotlin.js.internal.toNumber' call
    var self_0 = bigNumber & mask;
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    var chunk = Number(self_0);
    hashCode = imul_0(31, hashCode) + chunk | 0;
    // Inline function 'kotlin.js.internal.shr' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    bigNumber = bigNumber >> shiftNumber;
  }
  return imul_0(hashCode, signum);
}
function getSymbolHashCode(value) {
  var hashCodeMap = symbolIsSharable(value) ? getSymbolMap() : getSymbolWeakMap();
  var cachedHashCode = hashCodeMap.get(value);
  if (cachedHashCode !== VOID)
    return cachedHashCode;
  var hash = calculateRandomHash();
  hashCodeMap.set(value, hash);
  return hash;
}
function symbolIsSharable(symbol) {
  return Symbol.keyFor(symbol) != VOID;
}
function getSymbolMap() {
  if (symbolMap === VOID) {
    symbolMap = new Map();
  }
  return symbolMap;
}
function getSymbolWeakMap() {
  if (symbolWeakMap === VOID) {
    symbolWeakMap = new WeakMap();
  }
  return symbolWeakMap;
}
var symbolMap;
var symbolWeakMap;
function equals(obj1, obj2) {
  if (obj1 == null) {
    return obj2 == null;
  }
  if (obj2 == null) {
    return false;
  }
  if (typeof obj1 === 'object' && typeof obj1.equals === 'function') {
    return obj1.equals(obj2);
  }
  if (obj1 !== obj1) {
    return obj2 !== obj2;
  }
  if (typeof obj1 === 'number' && typeof obj2 === 'number') {
    var tmp;
    if (obj1 === obj2) {
      var tmp_0;
      if (obj1 !== 0) {
        tmp_0 = true;
      } else {
        // Inline function 'kotlin.js.asDynamic' call
        var tmp_1 = 1 / obj1;
        // Inline function 'kotlin.js.asDynamic' call
        tmp_0 = tmp_1 === 1 / obj2;
      }
      tmp = tmp_0;
    } else {
      tmp = false;
    }
    return tmp;
  }
  return obj1 === obj2;
}
function unboxIntrinsic(x) {
  var message = 'Should be lowered';
  throw IllegalStateException_init_$Create$_0(toString_1(message));
}
function captureStack(instance, constructorFunction) {
  if (Error.captureStackTrace != null) {
    Error.captureStackTrace(instance, constructorFunction);
  } else {
    // Inline function 'kotlin.js.asDynamic' call
    instance.stack = (new Error()).stack;
  }
}
function protoOf(constructor) {
  return constructor.prototype;
}
function defineMessage(message, cause) {
  var tmp;
  if (isUndefined(message)) {
    var tmp_0;
    if (isUndefined(cause)) {
      tmp_0 = message;
    } else {
      var tmp1_elvis_lhs = cause == null ? null : cause.toString();
      tmp_0 = tmp1_elvis_lhs == null ? VOID : tmp1_elvis_lhs;
    }
    tmp = tmp_0;
  } else {
    tmp = message == null ? VOID : message;
  }
  return tmp;
}
function isUndefined(value) {
  return value === VOID;
}
function extendThrowable(this_, message, cause) {
  defineFieldOnInstance(this_, 'message', defineMessage(message, cause));
  defineFieldOnInstance(this_, 'cause', cause);
  defineFieldOnInstance(this_, 'name', Object.getPrototypeOf(this_).constructor.name);
}
function defineFieldOnInstance(this_, name, value) {
  Object.defineProperty(this_, name, {configurable: true, writable: true, value: value});
}
function ensureNotNull(v) {
  var tmp;
  if (v == null) {
    THROW_NPE();
  } else {
    tmp = v;
  }
  return tmp;
}
function THROW_NPE() {
  throw NullPointerException_init_$Create$();
}
function noWhenBranchMatchedException() {
  throw NoWhenBranchMatchedException_init_$Create$();
}
function THROW_CCE() {
  throw ClassCastException_init_$Create$();
}
function throwUninitializedPropertyAccessException(name) {
  throw UninitializedPropertyAccessException_init_$Create$_0('lateinit property ' + name + ' has not been initialized');
}
function createMetadata(kind, name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity) {
  var undef = VOID;
  var iid = kind === 'interface' ? generateInterfaceId() : VOID;
  return {kind: kind, simpleName: name, associatedObjectKey: associatedObjectKey, associatedObjects: associatedObjects, suspendArity: suspendArity, $kClass$: undef, defaultConstructor: defaultConstructor, iid: iid};
}
function generateInterfaceId() {
  if (globalInterfaceId === VOID) {
    globalInterfaceId = 0;
  }
  // Inline function 'kotlin.js.unsafeCast' call
  globalInterfaceId = globalInterfaceId + 1 | 0;
  // Inline function 'kotlin.js.unsafeCast' call
  return globalInterfaceId;
}
var globalInterfaceId;
function initMetadataForClass(ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects) {
  var kind = 'class';
  initMetadataFor(kind, ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects);
}
function initMetadataFor(kind, ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects) {
  if (!(parent == null)) {
    ctor.prototype = Object.create(parent.prototype);
    ctor.prototype.constructor = ctor;
  }
  var metadata = createMetadata(kind, name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity);
  ctor.$metadata$ = metadata;
  if (!(interfaces == null)) {
    var receiver = !equals(metadata.iid, VOID) ? ctor : ctor.prototype;
    receiver.$imask$ = implement(interfaces);
  }
}
function initMetadataForObject(ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects) {
  var kind = 'object';
  initMetadataFor(kind, ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects);
}
function initMetadataForInterface(ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects) {
  var kind = 'interface';
  initMetadataFor(kind, ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects);
}
function initMetadataForLambda(ctor, parent, interfaces, suspendArity) {
  initMetadataForClass(ctor, 'Lambda', VOID, parent, interfaces, suspendArity, VOID, VOID);
}
function initMetadataForCoroutine(ctor, parent, interfaces, suspendArity) {
  initMetadataForClass(ctor, 'Coroutine', VOID, parent, interfaces, suspendArity, VOID, VOID);
}
function initMetadataForFunctionReference(ctor, parent, interfaces, suspendArity) {
  initMetadataForClass(ctor, 'FunctionReference', VOID, parent, interfaces, suspendArity, VOID, VOID);
}
function initMetadataForCompanion(ctor, parent, interfaces, suspendArity) {
  initMetadataForObject(ctor, 'Companion', VOID, parent, interfaces, suspendArity, VOID, VOID);
}
function numberToInt(a) {
  var tmp;
  if (a instanceof Long) {
    tmp = convertToInt(a);
  } else {
    tmp = doubleToInt(a);
  }
  return tmp;
}
function doubleToInt(a) {
  var tmp;
  if (a > 2147483647) {
    tmp = 2147483647;
  } else if (a < -2147483648) {
    tmp = -2147483648;
  } else {
    // Inline function 'kotlin.js.jsBitwiseOr' call
    tmp = a | 0;
  }
  return tmp;
}
function isArrayish(o) {
  return isJsArray(o) || isView(o);
}
function isJsArray(obj) {
  // Inline function 'kotlin.js.unsafeCast' call
  return Array.isArray(obj);
}
function isInterface(obj, iface) {
  return isInterfaceImpl(obj, iface.$metadata$.iid);
}
function isInterfaceImpl(obj, iface) {
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp0_elvis_lhs = obj.$imask$;
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return false;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var mask = tmp;
  return isBitSet(mask, iface);
}
function isArray(obj) {
  var tmp;
  if (isJsArray(obj)) {
    // Inline function 'kotlin.js.asDynamic' call
    tmp = !obj.$type$;
  } else {
    tmp = false;
  }
  return tmp;
}
function isNumber(a) {
  var tmp;
  if (typeof a === 'number') {
    tmp = true;
  } else {
    tmp = a instanceof Long;
  }
  return tmp;
}
function isCharSequence(value) {
  return typeof value === 'string' || isInterface(value, CharSequence);
}
function isBooleanArray(a) {
  return isJsArray(a) && a.$type$ === 'BooleanArray';
}
function isByteArray(a) {
  // Inline function 'kotlin.js.jsInstanceOf' call
  return a instanceof Int8Array;
}
function isShortArray(a) {
  // Inline function 'kotlin.js.jsInstanceOf' call
  return a instanceof Int16Array;
}
function isCharArray(a) {
  var tmp;
  // Inline function 'kotlin.js.jsInstanceOf' call
  if (a instanceof Uint16Array) {
    tmp = a.$type$ === 'CharArray';
  } else {
    tmp = false;
  }
  return tmp;
}
function isIntArray(a) {
  // Inline function 'kotlin.js.jsInstanceOf' call
  return a instanceof Int32Array;
}
function isFloatArray(a) {
  // Inline function 'kotlin.js.jsInstanceOf' call
  return a instanceof Float32Array;
}
function isLongArray(a) {
  return isJsArray(a) && a.$type$ === 'LongArray';
}
function isDoubleArray(a) {
  // Inline function 'kotlin.js.jsInstanceOf' call
  return a instanceof Float64Array;
}
function get_VOID() {
  _init_properties_void_kt__3zg9as();
  return VOID;
}
var VOID;
var properties_initialized_void_kt_e4ret2;
function _init_properties_void_kt__3zg9as() {
  if (!properties_initialized_void_kt_e4ret2) {
    properties_initialized_void_kt_e4ret2 = true;
    VOID = void 0;
  }
}
function copyOf(_this__u8e3s4, newSize) {
  // Inline function 'kotlin.require' call
  if (!(newSize >= 0)) {
    var message = 'Invalid new array size: ' + newSize + '.';
    throw IllegalArgumentException_init_$Create$_0(toString_1(message));
  }
  return fillFrom(_this__u8e3s4, new Int32Array(newSize));
}
function copyOf_0(_this__u8e3s4, newSize) {
  // Inline function 'kotlin.require' call
  if (!(newSize >= 0)) {
    var message = 'Invalid new array size: ' + newSize + '.';
    throw IllegalArgumentException_init_$Create$_0(toString_1(message));
  }
  return arrayCopyResize(_this__u8e3s4, newSize, null);
}
function Comparator() {
}
function isNaN_0(_this__u8e3s4) {
  return !(_this__u8e3s4 === _this__u8e3s4);
}
function takeHighestOneBit(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4 === 0) {
    tmp = 0;
  } else {
    // Inline function 'kotlin.countLeadingZeroBits' call
    tmp = 1 << (31 - clz32(_this__u8e3s4) | 0);
  }
  return tmp;
}
function Unit() {
}
protoOf(Unit).toString = function () {
  return 'kotlin.Unit';
};
var Unit_instance;
function Unit_getInstance() {
  return Unit_instance;
}
function collectionToArray(collection) {
  return collectionToArrayCommonImpl(collection);
}
function terminateCollectionToArray(collectionSize, array) {
  return array;
}
function arrayOfNulls(reference, size) {
  // Inline function 'kotlin.arrayOfNulls' call
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  return Array(size);
}
function arrayCopy(source, destination, destinationOffset, startIndex, endIndex) {
  Companion_instance_3.a1(startIndex, endIndex, source.length);
  var rangeSize = endIndex - startIndex | 0;
  Companion_instance_3.a1(destinationOffset, destinationOffset + rangeSize | 0, destination.length);
  if (isView(destination) && isView(source)) {
    // Inline function 'kotlin.js.asDynamic' call
    var subrange = source.subarray(startIndex, endIndex);
    // Inline function 'kotlin.js.asDynamic' call
    destination.set(subrange, destinationOffset);
  } else {
    if (!(source === destination) || destinationOffset <= startIndex) {
      var inductionVariable = 0;
      if (inductionVariable < rangeSize)
        do {
          var index = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          destination[destinationOffset + index | 0] = source[startIndex + index | 0];
        }
         while (inductionVariable < rangeSize);
    } else {
      var inductionVariable_0 = rangeSize - 1 | 0;
      if (0 <= inductionVariable_0)
        do {
          var index_0 = inductionVariable_0;
          inductionVariable_0 = inductionVariable_0 + -1 | 0;
          destination[destinationOffset + index_0 | 0] = source[startIndex + index_0 | 0];
        }
         while (0 <= inductionVariable_0);
    }
  }
}
function AbstractMutableCollection() {
  AbstractCollection.call(this);
}
protoOf(AbstractMutableCollection).toJSON = function () {
  return this.toArray();
};
protoOf(AbstractMutableCollection).c1 = function () {
};
function IteratorImpl($outer) {
  this.f1_1 = $outer;
  this.d1_1 = 0;
  this.e1_1 = -1;
}
protoOf(IteratorImpl).d = function () {
  return this.d1_1 < this.f1_1.f();
};
protoOf(IteratorImpl).e = function () {
  if (!this.d())
    throw NoSuchElementException_init_$Create$();
  var tmp = this;
  var _unary__edvuaz = this.d1_1;
  this.d1_1 = _unary__edvuaz + 1 | 0;
  tmp.e1_1 = _unary__edvuaz;
  return this.f1_1.i(this.e1_1);
};
function AbstractMutableList() {
  AbstractMutableCollection.call(this);
  this.g1_1 = 0;
}
protoOf(AbstractMutableList).b1 = function (element) {
  this.c1();
  this.h1(this.f(), element);
  return true;
};
protoOf(AbstractMutableList).c = function () {
  return new IteratorImpl(this);
};
protoOf(AbstractMutableList).h = function (element) {
  return this.j1(element) >= 0;
};
protoOf(AbstractMutableList).j1 = function (element) {
  var tmp$ret$1;
  $l$block: {
    // Inline function 'kotlin.collections.indexOfFirst' call
    var index = 0;
    var _iterator__ex2g4s = this.c();
    while (_iterator__ex2g4s.d()) {
      var item = _iterator__ex2g4s.e();
      if (equals(item, element)) {
        tmp$ret$1 = index;
        break $l$block;
      }
      index = index + 1 | 0;
    }
    tmp$ret$1 = -1;
  }
  return tmp$ret$1;
};
protoOf(AbstractMutableList).equals = function (other) {
  if (other === this)
    return true;
  if (!(!(other == null) ? isInterface(other, KtList) : false))
    return false;
  return Companion_instance_3.k1(this, other);
};
protoOf(AbstractMutableList).hashCode = function () {
  return Companion_instance_3.l1(this);
};
function AbstractMutableSet() {
  AbstractMutableCollection.call(this);
}
protoOf(AbstractMutableSet).equals = function (other) {
  if (other === this)
    return true;
  if (!(!(other == null) ? isInterface(other, KtSet) : false))
    return false;
  return Companion_instance_4.m1(this, other);
};
protoOf(AbstractMutableSet).hashCode = function () {
  return Companion_instance_4.n1(this);
};
function arrayOfUninitializedElements(capacity) {
  // Inline function 'kotlin.require' call
  if (!(capacity >= 0)) {
    var message = 'capacity must be non-negative.';
    throw IllegalArgumentException_init_$Create$_0(toString_1(message));
  }
  // Inline function 'kotlin.arrayOfNulls' call
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  return Array(capacity);
}
function resetRange(_this__u8e3s4, fromIndex, toIndex) {
  // Inline function 'kotlin.js.nativeFill' call
  // Inline function 'kotlin.js.asDynamic' call
  _this__u8e3s4.fill(null, fromIndex, toIndex);
}
function copyOfUninitializedElements(_this__u8e3s4, newSize) {
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  return copyOf_0(_this__u8e3s4, newSize);
}
function Companion_1() {
  Companion_instance_1 = this;
  var tmp = this;
  // Inline function 'kotlin.also' call
  var this_0 = ArrayList_init_$Create$_0(0);
  this_0.q1_1 = true;
  tmp.r1_1 = this_0;
}
var Companion_instance_1;
function Companion_getInstance_1() {
  if (Companion_instance_1 == null)
    new Companion_1();
  return Companion_instance_1;
}
function ArrayList_init_$Init$($this) {
  // Inline function 'kotlin.emptyArray' call
  var tmp$ret$0 = [];
  ArrayList.call($this, tmp$ret$0);
  return $this;
}
function ArrayList_init_$Create$() {
  return ArrayList_init_$Init$(objectCreate(protoOf(ArrayList)));
}
function ArrayList_init_$Init$_0(initialCapacity, $this) {
  // Inline function 'kotlin.emptyArray' call
  var tmp$ret$0 = [];
  ArrayList.call($this, tmp$ret$0);
  // Inline function 'kotlin.require' call
  if (!(initialCapacity >= 0)) {
    var message = 'Negative initial capacity: ' + initialCapacity;
    throw IllegalArgumentException_init_$Create$_0(toString_1(message));
  }
  return $this;
}
function ArrayList_init_$Create$_0(initialCapacity) {
  return ArrayList_init_$Init$_0(initialCapacity, objectCreate(protoOf(ArrayList)));
}
function rangeCheck($this, index) {
  // Inline function 'kotlin.apply' call
  Companion_instance_3.s1(index, $this.f());
  return index;
}
function insertionRangeCheck($this, index) {
  // Inline function 'kotlin.apply' call
  Companion_instance_3.t1(index, $this.f());
  return index;
}
function ArrayList(array) {
  Companion_getInstance_1();
  AbstractMutableList.call(this);
  this.p1_1 = array;
  this.q1_1 = false;
}
protoOf(ArrayList).f = function () {
  return this.p1_1.length;
};
protoOf(ArrayList).i = function (index) {
  var tmp = this.p1_1[rangeCheck(this, index)];
  return (tmp == null ? true : !(tmp == null)) ? tmp : THROW_CCE();
};
protoOf(ArrayList).b1 = function (element) {
  this.c1();
  // Inline function 'kotlin.js.asDynamic' call
  this.p1_1.push(element);
  this.g1_1 = this.g1_1 + 1 | 0;
  return true;
};
protoOf(ArrayList).h1 = function (index, element) {
  this.c1();
  // Inline function 'kotlin.js.asDynamic' call
  this.p1_1.splice(insertionRangeCheck(this, index), 0, element);
  this.g1_1 = this.g1_1 + 1 | 0;
};
protoOf(ArrayList).i1 = function (index) {
  this.c1();
  rangeCheck(this, index);
  this.g1_1 = this.g1_1 + 1 | 0;
  var tmp;
  if (index === get_lastIndex_0(this)) {
    // Inline function 'kotlin.js.asDynamic' call
    tmp = this.p1_1.pop();
  } else {
    // Inline function 'kotlin.js.asDynamic' call
    tmp = this.p1_1.splice(index, 1)[0];
  }
  return tmp;
};
protoOf(ArrayList).j1 = function (element) {
  return indexOf(this.p1_1, element);
};
protoOf(ArrayList).toString = function () {
  return arrayToString(this.p1_1);
};
protoOf(ArrayList).u1 = function () {
  return [].slice.call(this.p1_1);
};
protoOf(ArrayList).toArray = function () {
  return this.u1();
};
protoOf(ArrayList).c1 = function () {
  if (this.q1_1)
    throw UnsupportedOperationException_init_$Create$();
};
function HashSet_init_$Init$(map, $this) {
  AbstractMutableSet.call($this);
  HashSet.call($this);
  $this.v1_1 = map;
  return $this;
}
function HashSet_init_$Init$_0($this) {
  HashSet_init_$Init$(InternalHashMap_init_$Create$(), $this);
  return $this;
}
function HashSet_init_$Create$() {
  return HashSet_init_$Init$_0(objectCreate(protoOf(HashSet)));
}
function HashSet_init_$Init$_1(initialCapacity, loadFactor, $this) {
  HashSet_init_$Init$(InternalHashMap_init_$Create$_0(initialCapacity, loadFactor), $this);
  return $this;
}
function HashSet_init_$Init$_2(initialCapacity, $this) {
  HashSet_init_$Init$_1(initialCapacity, 1.0, $this);
  return $this;
}
function HashSet_init_$Create$_0(initialCapacity) {
  return HashSet_init_$Init$_2(initialCapacity, objectCreate(protoOf(HashSet)));
}
protoOf(HashSet).b1 = function (element) {
  return this.v1_1.w1(element, true) == null;
};
protoOf(HashSet).h = function (element) {
  return this.v1_1.x1(element);
};
protoOf(HashSet).g = function () {
  return this.v1_1.f() === 0;
};
protoOf(HashSet).c = function () {
  return this.v1_1.y1();
};
protoOf(HashSet).f = function () {
  return this.v1_1.f();
};
function HashSet() {
}
function computeHashSize($this, capacity) {
  return takeHighestOneBit(imul_0(coerceAtLeast(capacity, 1), 3));
}
function computeShift($this, hashSize) {
  // Inline function 'kotlin.countLeadingZeroBits' call
  return clz32(hashSize) + 1 | 0;
}
function checkForComodification($this) {
  if (!($this.j2_1.g2_1 === $this.l2_1))
    throw ConcurrentModificationException_init_$Create$_0('The backing map has been modified after this entry was obtained.');
}
function InternalHashMap_init_$Init$($this) {
  InternalHashMap_init_$Init$_0(8, $this);
  return $this;
}
function InternalHashMap_init_$Create$() {
  return InternalHashMap_init_$Init$(objectCreate(protoOf(InternalHashMap)));
}
function InternalHashMap_init_$Init$_0(initialCapacity, $this) {
  InternalHashMap.call($this, arrayOfUninitializedElements(initialCapacity), null, new Int32Array(initialCapacity), new Int32Array(computeHashSize(Companion_instance_2, initialCapacity)), 2, 0);
  return $this;
}
function InternalHashMap_init_$Init$_1(initialCapacity, loadFactor, $this) {
  InternalHashMap_init_$Init$_0(initialCapacity, $this);
  // Inline function 'kotlin.require' call
  if (!(loadFactor > 0)) {
    var message = 'Non-positive load factor: ' + loadFactor;
    throw IllegalArgumentException_init_$Create$_0(toString_1(message));
  }
  return $this;
}
function InternalHashMap_init_$Create$_0(initialCapacity, loadFactor) {
  return InternalHashMap_init_$Init$_1(initialCapacity, loadFactor, objectCreate(protoOf(InternalHashMap)));
}
function _get_capacity__a9k9f3($this) {
  return $this.z1_1.length;
}
function _get_hashSize__tftcho($this) {
  return $this.c2_1.length;
}
function registerModification($this) {
  $this.g2_1 = $this.g2_1 + 1 | 0;
}
function ensureExtraCapacity($this, n) {
  if (shouldCompact($this, n)) {
    compact($this, true);
  } else {
    ensureCapacity($this, $this.e2_1 + n | 0);
  }
}
function shouldCompact($this, extraCapacity) {
  var spareCapacity = _get_capacity__a9k9f3($this) - $this.e2_1 | 0;
  var gaps = $this.e2_1 - $this.f() | 0;
  return spareCapacity < extraCapacity && (gaps + spareCapacity | 0) >= extraCapacity && gaps >= (_get_capacity__a9k9f3($this) / 4 | 0);
}
function ensureCapacity($this, minCapacity) {
  if (minCapacity < 0)
    throw RuntimeException_init_$Create$_0('too many elements');
  if (minCapacity > _get_capacity__a9k9f3($this)) {
    var newSize = Companion_instance_3.m2(_get_capacity__a9k9f3($this), minCapacity);
    $this.z1_1 = copyOfUninitializedElements($this.z1_1, newSize);
    var tmp = $this;
    var tmp0_safe_receiver = $this.a2_1;
    tmp.a2_1 = tmp0_safe_receiver == null ? null : copyOfUninitializedElements(tmp0_safe_receiver, newSize);
    $this.b2_1 = copyOf($this.b2_1, newSize);
    var newHashSize = computeHashSize(Companion_instance_2, newSize);
    if (newHashSize > _get_hashSize__tftcho($this)) {
      rehash($this, newHashSize);
    }
  }
}
function allocateValuesArray($this) {
  var curValuesArray = $this.a2_1;
  if (!(curValuesArray == null))
    return curValuesArray;
  var newValuesArray = arrayOfUninitializedElements(_get_capacity__a9k9f3($this));
  $this.a2_1 = newValuesArray;
  return newValuesArray;
}
function hash($this, key) {
  return key == null ? 0 : imul_0(hashCode_0(key), -1640531527) >>> $this.f2_1 | 0;
}
function compact($this, updateHashArray) {
  var i = 0;
  var j = 0;
  var valuesArray = $this.a2_1;
  while (i < $this.e2_1) {
    var hash = $this.b2_1[i];
    if (hash >= 0) {
      $this.z1_1[j] = $this.z1_1[i];
      if (!(valuesArray == null)) {
        valuesArray[j] = valuesArray[i];
      }
      if (updateHashArray) {
        $this.b2_1[j] = hash;
        $this.c2_1[hash] = j + 1 | 0;
      }
      j = j + 1 | 0;
    }
    i = i + 1 | 0;
  }
  resetRange($this.z1_1, j, $this.e2_1);
  if (valuesArray == null)
    null;
  else {
    resetRange(valuesArray, j, $this.e2_1);
  }
  $this.e2_1 = j;
}
function rehash($this, newHashSize) {
  registerModification($this);
  if ($this.e2_1 > $this.h2_1) {
    compact($this, false);
  }
  $this.c2_1 = new Int32Array(newHashSize);
  $this.f2_1 = computeShift(Companion_instance_2, newHashSize);
  var i = 0;
  while (i < $this.e2_1) {
    var _unary__edvuaz = i;
    i = _unary__edvuaz + 1 | 0;
    if (!putRehash($this, _unary__edvuaz)) {
      throw IllegalStateException_init_$Create$_0('This cannot happen with fixed magic multiplier and grow-only hash array. Have object hashCodes changed?');
    }
  }
}
function putRehash($this, i) {
  var hash_0 = hash($this, $this.z1_1[i]);
  var probesLeft = $this.d2_1;
  while (true) {
    var index = $this.c2_1[hash_0];
    if (index === 0) {
      $this.c2_1[hash_0] = i + 1 | 0;
      $this.b2_1[i] = hash_0;
      return true;
    }
    probesLeft = probesLeft - 1 | 0;
    if (probesLeft < 0)
      return false;
    var _unary__edvuaz = hash_0;
    hash_0 = _unary__edvuaz - 1 | 0;
    if (_unary__edvuaz === 0)
      hash_0 = _get_hashSize__tftcho($this) - 1 | 0;
  }
}
function findKey($this, key) {
  var hash_0 = hash($this, key);
  var probesLeft = $this.d2_1;
  while (true) {
    var index = $this.c2_1[hash_0];
    if (index === 0)
      return -1;
    if (index > 0 && equals($this.z1_1[index - 1 | 0], key))
      return index - 1 | 0;
    probesLeft = probesLeft - 1 | 0;
    if (probesLeft < 0)
      return -1;
    var _unary__edvuaz = hash_0;
    hash_0 = _unary__edvuaz - 1 | 0;
    if (_unary__edvuaz === 0)
      hash_0 = _get_hashSize__tftcho($this) - 1 | 0;
  }
}
function addKey($this, key) {
  $this.n2();
  retry: while (true) {
    var hash_0 = hash($this, key);
    var tentativeMaxProbeDistance = coerceAtMost(imul_0($this.d2_1, 2), _get_hashSize__tftcho($this) / 2 | 0);
    var probeDistance = 0;
    while (true) {
      var index = $this.c2_1[hash_0];
      if (index <= 0) {
        if ($this.e2_1 >= _get_capacity__a9k9f3($this)) {
          ensureExtraCapacity($this, 1);
          continue retry;
        }
        var _unary__edvuaz = $this.e2_1;
        $this.e2_1 = _unary__edvuaz + 1 | 0;
        var putIndex = _unary__edvuaz;
        $this.z1_1[putIndex] = key;
        $this.b2_1[putIndex] = hash_0;
        $this.c2_1[hash_0] = putIndex + 1 | 0;
        $this.h2_1 = $this.h2_1 + 1 | 0;
        registerModification($this);
        if (probeDistance > $this.d2_1)
          $this.d2_1 = probeDistance;
        return putIndex;
      }
      if (equals($this.z1_1[index - 1 | 0], key)) {
        return -index | 0;
      }
      probeDistance = probeDistance + 1 | 0;
      if (probeDistance > tentativeMaxProbeDistance) {
        rehash($this, imul_0(_get_hashSize__tftcho($this), 2));
        continue retry;
      }
      var _unary__edvuaz_0 = hash_0;
      hash_0 = _unary__edvuaz_0 - 1 | 0;
      if (_unary__edvuaz_0 === 0)
        hash_0 = _get_hashSize__tftcho($this) - 1 | 0;
    }
  }
}
function contentEquals($this, other) {
  return $this.h2_1 === other.f() && $this.o2(other.m());
}
function Companion_2() {
  this.p2_1 = -1640531527;
  this.q2_1 = 8;
  this.r2_1 = 2;
  this.s2_1 = -1;
}
var Companion_instance_2;
function Companion_getInstance_2() {
  return Companion_instance_2;
}
function Itr(map) {
  this.t2_1 = map;
  this.u2_1 = 0;
  this.v2_1 = -1;
  this.w2_1 = this.t2_1.g2_1;
  this.x2();
}
protoOf(Itr).x2 = function () {
  while (this.u2_1 < this.t2_1.e2_1 && this.t2_1.b2_1[this.u2_1] < 0) {
    this.u2_1 = this.u2_1 + 1 | 0;
  }
};
protoOf(Itr).d = function () {
  return this.u2_1 < this.t2_1.e2_1;
};
protoOf(Itr).y2 = function () {
  if (!(this.t2_1.g2_1 === this.w2_1))
    throw ConcurrentModificationException_init_$Create$();
};
function KeysItr(map) {
  Itr.call(this, map);
}
protoOf(KeysItr).e = function () {
  this.y2();
  if (this.u2_1 >= this.t2_1.e2_1)
    throw NoSuchElementException_init_$Create$();
  var tmp = this;
  var _unary__edvuaz = this.u2_1;
  this.u2_1 = _unary__edvuaz + 1 | 0;
  tmp.v2_1 = _unary__edvuaz;
  var result = this.t2_1.z1_1[this.v2_1];
  this.x2();
  return result;
};
function EntriesItr(map) {
  Itr.call(this, map);
}
protoOf(EntriesItr).e = function () {
  this.y2();
  if (this.u2_1 >= this.t2_1.e2_1)
    throw NoSuchElementException_init_$Create$();
  var tmp = this;
  var _unary__edvuaz = this.u2_1;
  this.u2_1 = _unary__edvuaz + 1 | 0;
  tmp.v2_1 = _unary__edvuaz;
  var result = new EntryRef(this.t2_1, this.v2_1);
  this.x2();
  return result;
};
protoOf(EntriesItr).h3 = function () {
  if (this.u2_1 >= this.t2_1.e2_1)
    throw NoSuchElementException_init_$Create$();
  var tmp = this;
  var _unary__edvuaz = this.u2_1;
  this.u2_1 = _unary__edvuaz + 1 | 0;
  tmp.v2_1 = _unary__edvuaz;
  // Inline function 'kotlin.hashCode' call
  var tmp0_safe_receiver = this.t2_1.z1_1[this.v2_1];
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : hashCode_0(tmp0_safe_receiver);
  var tmp_0 = tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs;
  // Inline function 'kotlin.hashCode' call
  var tmp0_safe_receiver_0 = ensureNotNull(this.t2_1.a2_1)[this.v2_1];
  var tmp1_elvis_lhs_0 = tmp0_safe_receiver_0 == null ? null : hashCode_0(tmp0_safe_receiver_0);
  var result = tmp_0 ^ (tmp1_elvis_lhs_0 == null ? 0 : tmp1_elvis_lhs_0);
  this.x2();
  return result;
};
protoOf(EntriesItr).i3 = function (sb) {
  if (this.u2_1 >= this.t2_1.e2_1)
    throw NoSuchElementException_init_$Create$();
  var tmp = this;
  var _unary__edvuaz = this.u2_1;
  this.u2_1 = _unary__edvuaz + 1 | 0;
  tmp.v2_1 = _unary__edvuaz;
  var key = this.t2_1.z1_1[this.v2_1];
  if (equals(key, this.t2_1))
    sb.l3('(this Map)');
  else
    sb.k3(key);
  sb.m3(_Char___init__impl__6a9atx(61));
  var value = ensureNotNull(this.t2_1.a2_1)[this.v2_1];
  if (equals(value, this.t2_1))
    sb.l3('(this Map)');
  else
    sb.k3(value);
  this.x2();
};
function EntryRef(map, index) {
  this.j2_1 = map;
  this.k2_1 = index;
  this.l2_1 = this.j2_1.g2_1;
}
protoOf(EntryRef).k = function () {
  checkForComodification(this);
  return this.j2_1.z1_1[this.k2_1];
};
protoOf(EntryRef).l = function () {
  checkForComodification(this);
  return ensureNotNull(this.j2_1.a2_1)[this.k2_1];
};
protoOf(EntryRef).equals = function (other) {
  var tmp;
  var tmp_0;
  if (!(other == null) ? isInterface(other, Entry) : false) {
    tmp_0 = equals(other.k(), this.k());
  } else {
    tmp_0 = false;
  }
  if (tmp_0) {
    tmp = equals(other.l(), this.l());
  } else {
    tmp = false;
  }
  return tmp;
};
protoOf(EntryRef).hashCode = function () {
  // Inline function 'kotlin.hashCode' call
  var tmp0_safe_receiver = this.k();
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : hashCode_0(tmp0_safe_receiver);
  var tmp = tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs;
  // Inline function 'kotlin.hashCode' call
  var tmp0_safe_receiver_0 = this.l();
  var tmp1_elvis_lhs_0 = tmp0_safe_receiver_0 == null ? null : hashCode_0(tmp0_safe_receiver_0);
  return tmp ^ (tmp1_elvis_lhs_0 == null ? 0 : tmp1_elvis_lhs_0);
};
protoOf(EntryRef).toString = function () {
  return toString_0(this.k()) + '=' + toString_0(this.l());
};
function InternalHashMap(keysArray, valuesArray, presenceArray, hashArray, maxProbeDistance, length) {
  this.z1_1 = keysArray;
  this.a2_1 = valuesArray;
  this.b2_1 = presenceArray;
  this.c2_1 = hashArray;
  this.d2_1 = maxProbeDistance;
  this.e2_1 = length;
  this.f2_1 = computeShift(Companion_instance_2, _get_hashSize__tftcho(this));
  this.g2_1 = 0;
  this.h2_1 = 0;
  this.i2_1 = false;
}
protoOf(InternalHashMap).f = function () {
  return this.h2_1;
};
protoOf(InternalHashMap).x1 = function (key) {
  return findKey(this, key) >= 0;
};
protoOf(InternalHashMap).w1 = function (key, value) {
  var index = addKey(this, key);
  var valuesArray = allocateValuesArray(this);
  if (index < 0) {
    var oldValue = valuesArray[(-index | 0) - 1 | 0];
    valuesArray[(-index | 0) - 1 | 0] = value;
    return oldValue;
  } else {
    valuesArray[index] = value;
    return null;
  }
};
protoOf(InternalHashMap).equals = function (other) {
  var tmp;
  if (other === this) {
    tmp = true;
  } else {
    var tmp_0;
    if (!(other == null) ? isInterface(other, KtMap) : false) {
      tmp_0 = contentEquals(this, other);
    } else {
      tmp_0 = false;
    }
    tmp = tmp_0;
  }
  return tmp;
};
protoOf(InternalHashMap).hashCode = function () {
  var result = 0;
  var it = this.n3();
  while (it.d()) {
    result = result + it.h3() | 0;
  }
  return result;
};
protoOf(InternalHashMap).toString = function () {
  var sb = StringBuilder_init_$Create$(2 + imul_0(this.h2_1, 3) | 0);
  sb.l3('{');
  var i = 0;
  var it = this.n3();
  while (it.d()) {
    if (i > 0) {
      sb.l3(', ');
    }
    it.i3(sb);
    i = i + 1 | 0;
  }
  sb.l3('}');
  return sb.toString();
};
protoOf(InternalHashMap).n2 = function () {
  if (this.i2_1)
    throw UnsupportedOperationException_init_$Create$();
};
protoOf(InternalHashMap).o3 = function (entry) {
  var index = findKey(this, entry.k());
  if (index < 0)
    return false;
  return equals(ensureNotNull(this.a2_1)[index], entry.l());
};
protoOf(InternalHashMap).p3 = function (entry) {
  return this.o3(isInterface(entry, Entry) ? entry : THROW_CCE());
};
protoOf(InternalHashMap).y1 = function () {
  return new KeysItr(this);
};
protoOf(InternalHashMap).n3 = function () {
  return new EntriesItr(this);
};
function InternalMap() {
}
function LinkedHashSet_init_$Init$($this) {
  HashSet_init_$Init$_0($this);
  LinkedHashSet.call($this);
  return $this;
}
function LinkedHashSet_init_$Create$() {
  return LinkedHashSet_init_$Init$(objectCreate(protoOf(LinkedHashSet)));
}
function LinkedHashSet() {
}
function get_output() {
  _init_properties_console_kt__rfg7jv();
  return output;
}
var output;
function BaseOutput() {
}
protoOf(BaseOutput).q3 = function () {
  this.r3('\n');
};
protoOf(BaseOutput).s3 = function (message) {
  this.r3(message);
  this.q3();
};
function NodeJsOutput(outputStream) {
  BaseOutput.call(this);
  this.t3_1 = outputStream;
}
protoOf(NodeJsOutput).r3 = function (message) {
  // Inline function 'kotlin.io.String' call
  var tmp1_elvis_lhs = message == null ? null : toString_1(message);
  var messageString = tmp1_elvis_lhs == null ? 'null' : tmp1_elvis_lhs;
  this.t3_1.write(messageString);
};
function BufferedOutputToConsoleLog() {
  BufferedOutput.call(this);
}
protoOf(BufferedOutputToConsoleLog).r3 = function (message) {
  // Inline function 'kotlin.io.String' call
  var tmp1_elvis_lhs = message == null ? null : toString_1(message);
  var s = tmp1_elvis_lhs == null ? 'null' : tmp1_elvis_lhs;
  // Inline function 'kotlin.text.nativeLastIndexOf' call
  // Inline function 'kotlin.js.asDynamic' call
  var i = s.lastIndexOf('\n', 0);
  if (i >= 0) {
    this.v3_1 = this.v3_1 + substring(s, 0, i);
    this.w3();
    s = substring_0(s, i + 1 | 0);
  }
  this.v3_1 = this.v3_1 + s;
};
protoOf(BufferedOutputToConsoleLog).w3 = function () {
  console.log(this.v3_1);
  this.v3_1 = '';
};
function BufferedOutput() {
  BaseOutput.call(this);
  this.v3_1 = '';
}
protoOf(BufferedOutput).r3 = function (message) {
  var tmp = this;
  var tmp_0 = this.v3_1;
  // Inline function 'kotlin.io.String' call
  var tmp1_elvis_lhs = message == null ? null : toString_1(message);
  tmp.v3_1 = tmp_0 + (tmp1_elvis_lhs == null ? 'null' : tmp1_elvis_lhs);
};
function println(message) {
  _init_properties_console_kt__rfg7jv();
  get_output().s3(message);
}
var properties_initialized_console_kt_gll9dl;
function _init_properties_console_kt__rfg7jv() {
  if (!properties_initialized_console_kt_gll9dl) {
    properties_initialized_console_kt_gll9dl = true;
    // Inline function 'kotlin.run' call
    var isNode = typeof process !== 'undefined' && process.versions && !!process.versions.node;
    output = isNode ? new NodeJsOutput(process.stdout) : new BufferedOutputToConsoleLog();
  }
}
function CoroutineImpl(resultContinuation) {
  InterceptedCoroutine.call(this);
  this.y3_1 = resultContinuation;
  this.z3_1 = 0;
  this.a4_1 = 0;
  this.b4_1 = null;
  this.c4_1 = null;
  this.d4_1 = null;
  var tmp = this;
  var tmp0_safe_receiver = this.y3_1;
  tmp.e4_1 = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.f4();
}
protoOf(CoroutineImpl).f4 = function () {
  return ensureNotNull(this.e4_1);
};
protoOf(CoroutineImpl).g4 = function (result) {
  var current = this;
  // Inline function 'kotlin.Result.getOrNull' call
  var tmp;
  if (_Result___get_isFailure__impl__jpiriv(result)) {
    tmp = null;
  } else {
    var tmp_0 = _Result___get_value__impl__bjfvqg(result);
    tmp = (tmp_0 == null ? true : !(tmp_0 == null)) ? tmp_0 : THROW_CCE();
  }
  var currentResult = tmp;
  var currentException = Result__exceptionOrNull_impl_p6xea9(result);
  while (true) {
    // Inline function 'kotlin.with' call
    var $this$with = current;
    if (currentException == null) {
      $this$with.b4_1 = currentResult;
    } else {
      $this$with.z3_1 = $this$with.a4_1;
      $this$with.c4_1 = currentException;
    }
    try {
      var outcome = $this$with.h4();
      if (outcome === get_COROUTINE_SUSPENDED())
        return Unit_instance;
      currentResult = outcome;
      currentException = null;
    } catch ($p) {
      var exception = $p;
      currentResult = null;
      // Inline function 'kotlin.js.unsafeCast' call
      currentException = exception;
    }
    $this$with.j4();
    var completion = ensureNotNull($this$with.y3_1);
    if (completion instanceof CoroutineImpl) {
      current = completion;
    } else {
      if (!(currentException == null)) {
        // Inline function 'kotlin.coroutines.resumeWithException' call
        // Inline function 'kotlin.Companion.failure' call
        var exception_0 = ensureNotNull(currentException);
        var tmp$ret$2 = _Result___init__impl__xyqfz8(createFailure(exception_0));
        completion.k4(tmp$ret$2);
      } else {
        // Inline function 'kotlin.coroutines.resume' call
        // Inline function 'kotlin.Companion.success' call
        var value = currentResult;
        var tmp$ret$4 = _Result___init__impl__xyqfz8(value);
        completion.k4(tmp$ret$4);
      }
      return Unit_instance;
    }
  }
};
protoOf(CoroutineImpl).k4 = function (result) {
  return this.g4(result);
};
function CompletedContinuation() {
}
protoOf(CompletedContinuation).f4 = function () {
  var message = 'This continuation is already complete';
  throw IllegalStateException_init_$Create$_0(toString_1(message));
};
protoOf(CompletedContinuation).g4 = function (result) {
  // Inline function 'kotlin.error' call
  var message = 'This continuation is already complete';
  throw IllegalStateException_init_$Create$_0(toString_1(message));
};
protoOf(CompletedContinuation).k4 = function (result) {
  return this.g4(result);
};
protoOf(CompletedContinuation).toString = function () {
  return 'This continuation is already complete';
};
var CompletedContinuation_instance;
function CompletedContinuation_getInstance() {
  return CompletedContinuation_instance;
}
function InterceptedCoroutine() {
  this.i4_1 = null;
}
protoOf(InterceptedCoroutine).l4 = function () {
  var tmp0_elvis_lhs = this.i4_1;
  var tmp;
  if (tmp0_elvis_lhs == null) {
    var tmp1_safe_receiver = this.f4().m4(Key_instance);
    var tmp2_elvis_lhs = tmp1_safe_receiver == null ? null : tmp1_safe_receiver.n4(this);
    // Inline function 'kotlin.also' call
    var this_0 = tmp2_elvis_lhs == null ? this : tmp2_elvis_lhs;
    this.i4_1 = this_0;
    tmp = this_0;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
};
protoOf(InterceptedCoroutine).j4 = function () {
  var intercepted = this.i4_1;
  if (!(intercepted == null) && !(intercepted === this)) {
    ensureNotNull(this.f4().m4(Key_instance)).o4(intercepted);
  }
  this.i4_1 = CompletedContinuation_instance;
};
function CancellationException_init_$Init$($this) {
  IllegalStateException_init_$Init$($this);
  CancellationException.call($this);
  return $this;
}
function CancellationException_init_$Create$() {
  var tmp = CancellationException_init_$Init$(objectCreate(protoOf(CancellationException)));
  captureStack(tmp, CancellationException_init_$Create$);
  return tmp;
}
function CancellationException_init_$Init$_0(message, $this) {
  IllegalStateException_init_$Init$_0(message, $this);
  CancellationException.call($this);
  return $this;
}
function CancellationException_init_$Create$_0(message) {
  var tmp = CancellationException_init_$Init$_0(message, objectCreate(protoOf(CancellationException)));
  captureStack(tmp, CancellationException_init_$Create$_0);
  return tmp;
}
function CancellationException_init_$Init$_1(message, cause, $this) {
  IllegalStateException_init_$Init$_1(message, cause, $this);
  CancellationException.call($this);
  return $this;
}
function CancellationException() {
  captureStack(this, CancellationException);
}
function intercepted(_this__u8e3s4) {
  var tmp0_safe_receiver = _this__u8e3s4 instanceof InterceptedCoroutine ? _this__u8e3s4 : null;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.l4();
  return tmp1_elvis_lhs == null ? _this__u8e3s4 : tmp1_elvis_lhs;
}
function createCoroutineUnintercepted(_this__u8e3s4, receiver, completion) {
  // Inline function 'kotlin.coroutines.intrinsics.createCoroutineFromSuspendFunction' call
  return new createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$1(completion, _this__u8e3s4, receiver, completion);
}
function invokeSuspendSuperTypeWithReceiver(_this__u8e3s4, receiver, completion) {
  throw new NotImplementedError('It is intrinsic method');
}
function startCoroutineUninterceptedOrReturnNonGeneratorVersion(_this__u8e3s4, receiver, completion) {
  var tmp;
  if (!(completion instanceof InterceptedCoroutine)) {
    tmp = createSimpleCoroutineForSuspendFunction(completion);
  } else {
    tmp = completion;
  }
  var wrappedCompletion = tmp;
  // Inline function 'kotlin.js.asDynamic' call
  var a = _this__u8e3s4;
  return typeof a === 'function' ? a(receiver, wrappedCompletion) : _this__u8e3s4.r4(receiver, wrappedCompletion);
}
function createSimpleCoroutineForSuspendFunction(completion) {
  return new createSimpleCoroutineForSuspendFunction$1(completion);
}
function createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$1($completion, $this_createCoroutineUnintercepted, $receiver, $completion$1) {
  this.a5_1 = $this_createCoroutineUnintercepted;
  this.b5_1 = $receiver;
  this.c5_1 = $completion$1;
  CoroutineImpl.call(this, isInterface($completion, Continuation) ? $completion : THROW_CCE());
}
protoOf(createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$1).h4 = function () {
  if (this.c4_1 != null)
    throw this.c4_1;
  // Inline function 'kotlin.js.asDynamic' call
  var a = this.a5_1;
  return typeof a === 'function' ? a(this.b5_1, this.c5_1) : this.a5_1.r4(this.b5_1, this.c5_1);
};
function createSimpleCoroutineForSuspendFunction$1($completion) {
  CoroutineImpl.call(this, isInterface($completion, Continuation) ? $completion : THROW_CCE());
}
protoOf(createSimpleCoroutineForSuspendFunction$1).h4 = function () {
  if (this.c4_1 != null)
    throw this.c4_1;
  return this.b4_1;
};
function Exception_init_$Init$($this) {
  extendThrowable($this);
  Exception.call($this);
  return $this;
}
function Exception_init_$Create$() {
  var tmp = Exception_init_$Init$(objectCreate(protoOf(Exception)));
  captureStack(tmp, Exception_init_$Create$);
  return tmp;
}
function Exception_init_$Init$_0(message, $this) {
  extendThrowable($this, message);
  Exception.call($this);
  return $this;
}
function Exception_init_$Create$_0(message) {
  var tmp = Exception_init_$Init$_0(message, objectCreate(protoOf(Exception)));
  captureStack(tmp, Exception_init_$Create$_0);
  return tmp;
}
function Exception_init_$Init$_1(message, cause, $this) {
  extendThrowable($this, message, cause);
  Exception.call($this);
  return $this;
}
function Exception() {
  captureStack(this, Exception);
}
function IllegalArgumentException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  IllegalArgumentException.call($this);
  return $this;
}
function IllegalArgumentException_init_$Create$() {
  var tmp = IllegalArgumentException_init_$Init$(objectCreate(protoOf(IllegalArgumentException)));
  captureStack(tmp, IllegalArgumentException_init_$Create$);
  return tmp;
}
function IllegalArgumentException_init_$Init$_0(message, $this) {
  RuntimeException_init_$Init$_0(message, $this);
  IllegalArgumentException.call($this);
  return $this;
}
function IllegalArgumentException_init_$Create$_0(message) {
  var tmp = IllegalArgumentException_init_$Init$_0(message, objectCreate(protoOf(IllegalArgumentException)));
  captureStack(tmp, IllegalArgumentException_init_$Create$_0);
  return tmp;
}
function IllegalArgumentException() {
  captureStack(this, IllegalArgumentException);
}
function IllegalStateException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  IllegalStateException.call($this);
  return $this;
}
function IllegalStateException_init_$Create$() {
  var tmp = IllegalStateException_init_$Init$(objectCreate(protoOf(IllegalStateException)));
  captureStack(tmp, IllegalStateException_init_$Create$);
  return tmp;
}
function IllegalStateException_init_$Init$_0(message, $this) {
  RuntimeException_init_$Init$_0(message, $this);
  IllegalStateException.call($this);
  return $this;
}
function IllegalStateException_init_$Create$_0(message) {
  var tmp = IllegalStateException_init_$Init$_0(message, objectCreate(protoOf(IllegalStateException)));
  captureStack(tmp, IllegalStateException_init_$Create$_0);
  return tmp;
}
function IllegalStateException_init_$Init$_1(message, cause, $this) {
  RuntimeException_init_$Init$_1(message, cause, $this);
  IllegalStateException.call($this);
  return $this;
}
function IllegalStateException_init_$Create$_1(message, cause) {
  var tmp = IllegalStateException_init_$Init$_1(message, cause, objectCreate(protoOf(IllegalStateException)));
  captureStack(tmp, IllegalStateException_init_$Create$_1);
  return tmp;
}
function IllegalStateException() {
  captureStack(this, IllegalStateException);
}
function UnsupportedOperationException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  UnsupportedOperationException.call($this);
  return $this;
}
function UnsupportedOperationException_init_$Create$() {
  var tmp = UnsupportedOperationException_init_$Init$(objectCreate(protoOf(UnsupportedOperationException)));
  captureStack(tmp, UnsupportedOperationException_init_$Create$);
  return tmp;
}
function UnsupportedOperationException_init_$Init$_0(message, $this) {
  RuntimeException_init_$Init$_0(message, $this);
  UnsupportedOperationException.call($this);
  return $this;
}
function UnsupportedOperationException_init_$Create$_0(message) {
  var tmp = UnsupportedOperationException_init_$Init$_0(message, objectCreate(protoOf(UnsupportedOperationException)));
  captureStack(tmp, UnsupportedOperationException_init_$Create$_0);
  return tmp;
}
function UnsupportedOperationException() {
  captureStack(this, UnsupportedOperationException);
}
function RuntimeException_init_$Init$($this) {
  Exception_init_$Init$($this);
  RuntimeException.call($this);
  return $this;
}
function RuntimeException_init_$Create$() {
  var tmp = RuntimeException_init_$Init$(objectCreate(protoOf(RuntimeException)));
  captureStack(tmp, RuntimeException_init_$Create$);
  return tmp;
}
function RuntimeException_init_$Init$_0(message, $this) {
  Exception_init_$Init$_0(message, $this);
  RuntimeException.call($this);
  return $this;
}
function RuntimeException_init_$Create$_0(message) {
  var tmp = RuntimeException_init_$Init$_0(message, objectCreate(protoOf(RuntimeException)));
  captureStack(tmp, RuntimeException_init_$Create$_0);
  return tmp;
}
function RuntimeException_init_$Init$_1(message, cause, $this) {
  Exception_init_$Init$_1(message, cause, $this);
  RuntimeException.call($this);
  return $this;
}
function RuntimeException_init_$Create$_1(message, cause) {
  var tmp = RuntimeException_init_$Init$_1(message, cause, objectCreate(protoOf(RuntimeException)));
  captureStack(tmp, RuntimeException_init_$Create$_1);
  return tmp;
}
function RuntimeException() {
  captureStack(this, RuntimeException);
}
function NoSuchElementException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  NoSuchElementException.call($this);
  return $this;
}
function NoSuchElementException_init_$Create$() {
  var tmp = NoSuchElementException_init_$Init$(objectCreate(protoOf(NoSuchElementException)));
  captureStack(tmp, NoSuchElementException_init_$Create$);
  return tmp;
}
function NoSuchElementException_init_$Init$_0(message, $this) {
  RuntimeException_init_$Init$_0(message, $this);
  NoSuchElementException.call($this);
  return $this;
}
function NoSuchElementException_init_$Create$_0(message) {
  var tmp = NoSuchElementException_init_$Init$_0(message, objectCreate(protoOf(NoSuchElementException)));
  captureStack(tmp, NoSuchElementException_init_$Create$_0);
  return tmp;
}
function NoSuchElementException() {
  captureStack(this, NoSuchElementException);
}
function Error_init_$Init$($this) {
  extendThrowable($this);
  Error_0.call($this);
  return $this;
}
function Error_init_$Create$() {
  var tmp = Error_init_$Init$(objectCreate(protoOf(Error_0)));
  captureStack(tmp, Error_init_$Create$);
  return tmp;
}
function Error_init_$Init$_0(message, $this) {
  extendThrowable($this, message);
  Error_0.call($this);
  return $this;
}
function Error_init_$Init$_1(message, cause, $this) {
  extendThrowable($this, message, cause);
  Error_0.call($this);
  return $this;
}
function Error_0() {
  captureStack(this, Error_0);
}
function IndexOutOfBoundsException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  IndexOutOfBoundsException.call($this);
  return $this;
}
function IndexOutOfBoundsException_init_$Create$() {
  var tmp = IndexOutOfBoundsException_init_$Init$(objectCreate(protoOf(IndexOutOfBoundsException)));
  captureStack(tmp, IndexOutOfBoundsException_init_$Create$);
  return tmp;
}
function IndexOutOfBoundsException_init_$Init$_0(message, $this) {
  RuntimeException_init_$Init$_0(message, $this);
  IndexOutOfBoundsException.call($this);
  return $this;
}
function IndexOutOfBoundsException_init_$Create$_0(message) {
  var tmp = IndexOutOfBoundsException_init_$Init$_0(message, objectCreate(protoOf(IndexOutOfBoundsException)));
  captureStack(tmp, IndexOutOfBoundsException_init_$Create$_0);
  return tmp;
}
function IndexOutOfBoundsException() {
  captureStack(this, IndexOutOfBoundsException);
}
function ConcurrentModificationException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  ConcurrentModificationException.call($this);
  return $this;
}
function ConcurrentModificationException_init_$Create$() {
  var tmp = ConcurrentModificationException_init_$Init$(objectCreate(protoOf(ConcurrentModificationException)));
  captureStack(tmp, ConcurrentModificationException_init_$Create$);
  return tmp;
}
function ConcurrentModificationException_init_$Init$_0(message, $this) {
  RuntimeException_init_$Init$_0(message, $this);
  ConcurrentModificationException.call($this);
  return $this;
}
function ConcurrentModificationException_init_$Create$_0(message) {
  var tmp = ConcurrentModificationException_init_$Init$_0(message, objectCreate(protoOf(ConcurrentModificationException)));
  captureStack(tmp, ConcurrentModificationException_init_$Create$_0);
  return tmp;
}
function ConcurrentModificationException() {
  captureStack(this, ConcurrentModificationException);
}
function NullPointerException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  NullPointerException.call($this);
  return $this;
}
function NullPointerException_init_$Create$() {
  var tmp = NullPointerException_init_$Init$(objectCreate(protoOf(NullPointerException)));
  captureStack(tmp, NullPointerException_init_$Create$);
  return tmp;
}
function NullPointerException() {
  captureStack(this, NullPointerException);
}
function NoWhenBranchMatchedException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  NoWhenBranchMatchedException.call($this);
  return $this;
}
function NoWhenBranchMatchedException_init_$Create$() {
  var tmp = NoWhenBranchMatchedException_init_$Init$(objectCreate(protoOf(NoWhenBranchMatchedException)));
  captureStack(tmp, NoWhenBranchMatchedException_init_$Create$);
  return tmp;
}
function NoWhenBranchMatchedException() {
  captureStack(this, NoWhenBranchMatchedException);
}
function ClassCastException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  ClassCastException.call($this);
  return $this;
}
function ClassCastException_init_$Create$() {
  var tmp = ClassCastException_init_$Init$(objectCreate(protoOf(ClassCastException)));
  captureStack(tmp, ClassCastException_init_$Create$);
  return tmp;
}
function ClassCastException() {
  captureStack(this, ClassCastException);
}
function UninitializedPropertyAccessException_init_$Init$($this) {
  RuntimeException_init_$Init$($this);
  UninitializedPropertyAccessException.call($this);
  return $this;
}
function UninitializedPropertyAccessException_init_$Create$() {
  var tmp = UninitializedPropertyAccessException_init_$Init$(objectCreate(protoOf(UninitializedPropertyAccessException)));
  captureStack(tmp, UninitializedPropertyAccessException_init_$Create$);
  return tmp;
}
function UninitializedPropertyAccessException_init_$Init$_0(message, $this) {
  RuntimeException_init_$Init$_0(message, $this);
  UninitializedPropertyAccessException.call($this);
  return $this;
}
function UninitializedPropertyAccessException_init_$Create$_0(message) {
  var tmp = UninitializedPropertyAccessException_init_$Init$_0(message, objectCreate(protoOf(UninitializedPropertyAccessException)));
  captureStack(tmp, UninitializedPropertyAccessException_init_$Create$_0);
  return tmp;
}
function UninitializedPropertyAccessException() {
  captureStack(this, UninitializedPropertyAccessException);
}
function fillFrom(src, dst) {
  var srcLen = src.length;
  var dstLen = dst.length;
  var index = 0;
  // Inline function 'kotlin.js.unsafeCast' call
  var arr = dst;
  while (index < srcLen && index < dstLen) {
    var tmp = index;
    var _unary__edvuaz = index;
    index = _unary__edvuaz + 1 | 0;
    arr[tmp] = src[_unary__edvuaz];
  }
  return dst;
}
function arrayCopyResize(source, newSize, defaultValue) {
  // Inline function 'kotlin.js.unsafeCast' call
  var result = source.slice(0, newSize);
  // Inline function 'kotlin.copyArrayType' call
  if (source.$type$ !== undefined) {
    result.$type$ = source.$type$;
  }
  var index = source.length;
  if (newSize > index) {
    // Inline function 'kotlin.js.asDynamic' call
    result.length = newSize;
    while (index < newSize) {
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      result[_unary__edvuaz] = defaultValue;
    }
  }
  return result;
}
function KClass() {
}
function KClassImpl() {
}
protoOf(KClassImpl).equals = function (other) {
  var tmp;
  if (other instanceof NothingKClassImpl) {
    tmp = false;
  } else {
    if (other instanceof KClassImpl) {
      tmp = equals(this.m5(), other.m5());
    } else {
      tmp = false;
    }
  }
  return tmp;
};
protoOf(KClassImpl).hashCode = function () {
  var tmp0_safe_receiver = this.l5();
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : getStringHashCode(tmp0_safe_receiver);
  return tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs;
};
protoOf(KClassImpl).toString = function () {
  return 'class ' + this.l5();
};
function NothingKClassImpl() {
  NothingKClassImpl_instance = this;
  KClassImpl.call(this);
  this.n5_1 = 'Nothing';
}
protoOf(NothingKClassImpl).l5 = function () {
  return this.n5_1;
};
protoOf(NothingKClassImpl).m5 = function () {
  throw UnsupportedOperationException_init_$Create$_0("There's no native JS class for Nothing type");
};
protoOf(NothingKClassImpl).equals = function (other) {
  return other === this;
};
protoOf(NothingKClassImpl).hashCode = function () {
  return 0;
};
var NothingKClassImpl_instance;
function NothingKClassImpl_getInstance() {
  if (NothingKClassImpl_instance == null)
    new NothingKClassImpl();
  return NothingKClassImpl_instance;
}
function PrimitiveKClassImpl(jClass, givenSimpleName, isInstanceFunction) {
  KClassImpl.call(this);
  this.o5_1 = jClass;
  this.p5_1 = givenSimpleName;
  this.q5_1 = isInstanceFunction;
}
protoOf(PrimitiveKClassImpl).m5 = function () {
  return this.o5_1;
};
protoOf(PrimitiveKClassImpl).equals = function (other) {
  if (!(other instanceof PrimitiveKClassImpl))
    return false;
  return protoOf(KClassImpl).equals.call(this, other) && this.p5_1 === other.p5_1;
};
protoOf(PrimitiveKClassImpl).l5 = function () {
  return this.p5_1;
};
function SimpleKClassImpl(jClass) {
  KClassImpl.call(this);
  this.r5_1 = jClass;
  var tmp = this;
  // Inline function 'kotlin.js.asDynamic' call
  var tmp0_safe_receiver = this.r5_1.$metadata$;
  // Inline function 'kotlin.js.unsafeCast' call
  tmp.s5_1 = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.simpleName;
}
protoOf(SimpleKClassImpl).m5 = function () {
  return this.r5_1;
};
protoOf(SimpleKClassImpl).l5 = function () {
  return this.s5_1;
};
function get_functionClasses() {
  _init_properties_primitives_kt__3fums4();
  return functionClasses;
}
var functionClasses;
function PrimitiveClasses$anyClass$lambda(it) {
  return !(it == null);
}
function PrimitiveClasses$numberClass$lambda(it) {
  return isNumber(it);
}
function PrimitiveClasses$booleanClass$lambda(it) {
  return !(it == null) ? typeof it === 'boolean' : false;
}
function PrimitiveClasses$byteClass$lambda(it) {
  return !(it == null) ? typeof it === 'number' : false;
}
function PrimitiveClasses$shortClass$lambda(it) {
  return !(it == null) ? typeof it === 'number' : false;
}
function PrimitiveClasses$intClass$lambda(it) {
  return !(it == null) ? typeof it === 'number' : false;
}
function PrimitiveClasses$longClass$lambda(it) {
  return it instanceof Long;
}
function PrimitiveClasses$floatClass$lambda(it) {
  return !(it == null) ? typeof it === 'number' : false;
}
function PrimitiveClasses$doubleClass$lambda(it) {
  return !(it == null) ? typeof it === 'number' : false;
}
function PrimitiveClasses$arrayClass$lambda(it) {
  return !(it == null) ? isArray(it) : false;
}
function PrimitiveClasses$stringClass$lambda(it) {
  return !(it == null) ? typeof it === 'string' : false;
}
function PrimitiveClasses$throwableClass$lambda(it) {
  return it instanceof Error;
}
function PrimitiveClasses$booleanArrayClass$lambda(it) {
  return !(it == null) ? isBooleanArray(it) : false;
}
function PrimitiveClasses$charArrayClass$lambda(it) {
  return !(it == null) ? isCharArray(it) : false;
}
function PrimitiveClasses$byteArrayClass$lambda(it) {
  return !(it == null) ? isByteArray(it) : false;
}
function PrimitiveClasses$shortArrayClass$lambda(it) {
  return !(it == null) ? isShortArray(it) : false;
}
function PrimitiveClasses$intArrayClass$lambda(it) {
  return !(it == null) ? isIntArray(it) : false;
}
function PrimitiveClasses$longArrayClass$lambda(it) {
  return !(it == null) ? isLongArray(it) : false;
}
function PrimitiveClasses$floatArrayClass$lambda(it) {
  return !(it == null) ? isFloatArray(it) : false;
}
function PrimitiveClasses$doubleArrayClass$lambda(it) {
  return !(it == null) ? isDoubleArray(it) : false;
}
function PrimitiveClasses$functionClass$lambda($arity) {
  return function (it) {
    var tmp;
    if (typeof it === 'function') {
      // Inline function 'kotlin.js.asDynamic' call
      tmp = it.length === $arity;
    } else {
      tmp = false;
    }
    return tmp;
  };
}
function PrimitiveClasses() {
  PrimitiveClasses_instance = this;
  var tmp = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_0 = Object;
  tmp.anyClass = new PrimitiveKClassImpl(tmp_0, 'Any', PrimitiveClasses$anyClass$lambda);
  var tmp_1 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_2 = Number;
  tmp_1.numberClass = new PrimitiveKClassImpl(tmp_2, 'Number', PrimitiveClasses$numberClass$lambda);
  this.nothingClass = NothingKClassImpl_getInstance();
  var tmp_3 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_4 = Boolean;
  tmp_3.booleanClass = new PrimitiveKClassImpl(tmp_4, 'Boolean', PrimitiveClasses$booleanClass$lambda);
  var tmp_5 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_6 = Number;
  tmp_5.byteClass = new PrimitiveKClassImpl(tmp_6, 'Byte', PrimitiveClasses$byteClass$lambda);
  var tmp_7 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_8 = Number;
  tmp_7.shortClass = new PrimitiveKClassImpl(tmp_8, 'Short', PrimitiveClasses$shortClass$lambda);
  var tmp_9 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_10 = Number;
  tmp_9.intClass = new PrimitiveKClassImpl(tmp_10, 'Int', PrimitiveClasses$intClass$lambda);
  var tmp_11 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  var tmp_12 = typeof BigInt === 'undefined' ? VOID : BigInt;
  tmp_11.longClass = new PrimitiveKClassImpl(tmp_12, 'Long', PrimitiveClasses$longClass$lambda);
  var tmp_13 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_14 = Number;
  tmp_13.floatClass = new PrimitiveKClassImpl(tmp_14, 'Float', PrimitiveClasses$floatClass$lambda);
  var tmp_15 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_16 = Number;
  tmp_15.doubleClass = new PrimitiveKClassImpl(tmp_16, 'Double', PrimitiveClasses$doubleClass$lambda);
  var tmp_17 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_18 = Array;
  tmp_17.arrayClass = new PrimitiveKClassImpl(tmp_18, 'Array', PrimitiveClasses$arrayClass$lambda);
  var tmp_19 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_20 = String;
  tmp_19.stringClass = new PrimitiveKClassImpl(tmp_20, 'String', PrimitiveClasses$stringClass$lambda);
  var tmp_21 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_22 = Error;
  tmp_21.throwableClass = new PrimitiveKClassImpl(tmp_22, 'Throwable', PrimitiveClasses$throwableClass$lambda);
  var tmp_23 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_24 = Array;
  tmp_23.booleanArrayClass = new PrimitiveKClassImpl(tmp_24, 'BooleanArray', PrimitiveClasses$booleanArrayClass$lambda);
  var tmp_25 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_26 = Uint16Array;
  tmp_25.charArrayClass = new PrimitiveKClassImpl(tmp_26, 'CharArray', PrimitiveClasses$charArrayClass$lambda);
  var tmp_27 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_28 = Int8Array;
  tmp_27.byteArrayClass = new PrimitiveKClassImpl(tmp_28, 'ByteArray', PrimitiveClasses$byteArrayClass$lambda);
  var tmp_29 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_30 = Int16Array;
  tmp_29.shortArrayClass = new PrimitiveKClassImpl(tmp_30, 'ShortArray', PrimitiveClasses$shortArrayClass$lambda);
  var tmp_31 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_32 = Int32Array;
  tmp_31.intArrayClass = new PrimitiveKClassImpl(tmp_32, 'IntArray', PrimitiveClasses$intArrayClass$lambda);
  var tmp_33 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_34 = Array;
  tmp_33.longArrayClass = new PrimitiveKClassImpl(tmp_34, 'LongArray', PrimitiveClasses$longArrayClass$lambda);
  var tmp_35 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_36 = Float32Array;
  tmp_35.floatArrayClass = new PrimitiveKClassImpl(tmp_36, 'FloatArray', PrimitiveClasses$floatArrayClass$lambda);
  var tmp_37 = this;
  // Inline function 'kotlin.js.unsafeCast' call
  var tmp_38 = Float64Array;
  tmp_37.doubleArrayClass = new PrimitiveKClassImpl(tmp_38, 'DoubleArray', PrimitiveClasses$doubleArrayClass$lambda);
}
protoOf(PrimitiveClasses).t5 = function () {
  return this.anyClass;
};
protoOf(PrimitiveClasses).u5 = function () {
  return this.numberClass;
};
protoOf(PrimitiveClasses).v5 = function () {
  return this.nothingClass;
};
protoOf(PrimitiveClasses).w5 = function () {
  return this.booleanClass;
};
protoOf(PrimitiveClasses).x5 = function () {
  return this.byteClass;
};
protoOf(PrimitiveClasses).y5 = function () {
  return this.shortClass;
};
protoOf(PrimitiveClasses).z5 = function () {
  return this.intClass;
};
protoOf(PrimitiveClasses).a6 = function () {
  return this.longClass;
};
protoOf(PrimitiveClasses).b6 = function () {
  return this.floatClass;
};
protoOf(PrimitiveClasses).c6 = function () {
  return this.doubleClass;
};
protoOf(PrimitiveClasses).d6 = function () {
  return this.arrayClass;
};
protoOf(PrimitiveClasses).e6 = function () {
  return this.stringClass;
};
protoOf(PrimitiveClasses).f6 = function () {
  return this.throwableClass;
};
protoOf(PrimitiveClasses).g6 = function () {
  return this.booleanArrayClass;
};
protoOf(PrimitiveClasses).h6 = function () {
  return this.charArrayClass;
};
protoOf(PrimitiveClasses).i6 = function () {
  return this.byteArrayClass;
};
protoOf(PrimitiveClasses).j6 = function () {
  return this.shortArrayClass;
};
protoOf(PrimitiveClasses).k6 = function () {
  return this.intArrayClass;
};
protoOf(PrimitiveClasses).l6 = function () {
  return this.longArrayClass;
};
protoOf(PrimitiveClasses).m6 = function () {
  return this.floatArrayClass;
};
protoOf(PrimitiveClasses).n6 = function () {
  return this.doubleArrayClass;
};
protoOf(PrimitiveClasses).functionClass = function (arity) {
  var tmp0_elvis_lhs = get_functionClasses()[arity];
  var tmp;
  if (tmp0_elvis_lhs == null) {
    // Inline function 'kotlin.run' call
    // Inline function 'kotlin.js.unsafeCast' call
    var tmp_0 = Function;
    var tmp_1 = 'Function' + arity;
    var result = new PrimitiveKClassImpl(tmp_0, tmp_1, PrimitiveClasses$functionClass$lambda(arity));
    // Inline function 'kotlin.js.asDynamic' call
    get_functionClasses()[arity] = result;
    tmp = result;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
};
var PrimitiveClasses_instance;
function PrimitiveClasses_getInstance() {
  if (PrimitiveClasses_instance == null)
    new PrimitiveClasses();
  return PrimitiveClasses_instance;
}
var properties_initialized_primitives_kt_jle18u;
function _init_properties_primitives_kt__3fums4() {
  if (!properties_initialized_primitives_kt_jle18u) {
    properties_initialized_primitives_kt_jle18u = true;
    // Inline function 'kotlin.arrayOfNulls' call
    functionClasses = Array(0);
  }
}
function getKClass(jClass) {
  if (jClass === String) {
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return PrimitiveClasses_getInstance().stringClass;
  }
  // Inline function 'kotlin.js.asDynamic' call
  var metadata = jClass.$metadata$;
  var tmp;
  if (metadata != null) {
    var tmp_0;
    if (metadata.$kClass$ == null) {
      var kClass = new SimpleKClassImpl(jClass);
      metadata.$kClass$ = kClass;
      tmp_0 = kClass;
    } else {
      tmp_0 = metadata.$kClass$;
    }
    tmp = tmp_0;
  } else {
    tmp = new SimpleKClassImpl(jClass);
  }
  return tmp;
}
function getKClassFromExpression(e) {
  var tmp;
  switch (typeof e) {
    case 'string':
      tmp = PrimitiveClasses_getInstance().stringClass;
      break;
    case 'number':
      var tmp_0;
      // Inline function 'kotlin.js.jsBitwiseOr' call

      // Inline function 'kotlin.js.asDynamic' call

      if ((e | 0) === e) {
        tmp_0 = PrimitiveClasses_getInstance().intClass;
      } else {
        tmp_0 = PrimitiveClasses_getInstance().doubleClass;
      }

      tmp = tmp_0;
      break;
    case 'boolean':
      tmp = PrimitiveClasses_getInstance().booleanClass;
      break;
    case 'function':
      var tmp_1 = PrimitiveClasses_getInstance();
      // Inline function 'kotlin.js.asDynamic' call

      tmp = tmp_1.functionClass(e.length);
      break;
    default:
      var tmp_2;
      if (isBooleanArray(e)) {
        tmp_2 = PrimitiveClasses_getInstance().booleanArrayClass;
      } else {
        if (isCharArray(e)) {
          tmp_2 = PrimitiveClasses_getInstance().charArrayClass;
        } else {
          if (isByteArray(e)) {
            tmp_2 = PrimitiveClasses_getInstance().byteArrayClass;
          } else {
            if (isShortArray(e)) {
              tmp_2 = PrimitiveClasses_getInstance().shortArrayClass;
            } else {
              if (isIntArray(e)) {
                tmp_2 = PrimitiveClasses_getInstance().intArrayClass;
              } else {
                if (isLongArray(e)) {
                  tmp_2 = PrimitiveClasses_getInstance().longArrayClass;
                } else {
                  if (isFloatArray(e)) {
                    tmp_2 = PrimitiveClasses_getInstance().floatArrayClass;
                  } else {
                    if (isDoubleArray(e)) {
                      tmp_2 = PrimitiveClasses_getInstance().doubleArrayClass;
                    } else {
                      if (isInterface(e, KClass)) {
                        tmp_2 = getKClass(KClass);
                      } else {
                        if (isArray(e)) {
                          tmp_2 = PrimitiveClasses_getInstance().arrayClass;
                        } else {
                          var constructor = Object.getPrototypeOf(e).constructor;
                          var tmp_3;
                          if (constructor === Object) {
                            tmp_3 = PrimitiveClasses_getInstance().anyClass;
                          } else if (constructor === Error) {
                            tmp_3 = PrimitiveClasses_getInstance().throwableClass;
                          } else {
                            var jsClass = constructor;
                            tmp_3 = getKClass(jsClass);
                          }
                          tmp_2 = tmp_3;
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      tmp = tmp_2;
      break;
  }
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  return tmp;
}
function StringBuilder_init_$Init$(capacity, $this) {
  StringBuilder_init_$Init$_0($this);
  return $this;
}
function StringBuilder_init_$Create$(capacity) {
  return StringBuilder_init_$Init$(capacity, objectCreate(protoOf(StringBuilder)));
}
function StringBuilder_init_$Init$_0($this) {
  StringBuilder.call($this, '');
  return $this;
}
function StringBuilder_init_$Create$_0() {
  return StringBuilder_init_$Init$_0(objectCreate(protoOf(StringBuilder)));
}
function StringBuilder(content) {
  this.j3_1 = content;
}
protoOf(StringBuilder).a = function () {
  // Inline function 'kotlin.js.asDynamic' call
  return this.j3_1.length;
};
protoOf(StringBuilder).m3 = function (value) {
  this.j3_1 = this.j3_1 + toString(value);
  return this;
};
protoOf(StringBuilder).b = function (value) {
  this.j3_1 = this.j3_1 + toString_0(value);
  return this;
};
protoOf(StringBuilder).k3 = function (value) {
  this.j3_1 = this.j3_1 + toString_0(value);
  return this;
};
protoOf(StringBuilder).l3 = function (value) {
  var tmp = this;
  var tmp_0 = this.j3_1;
  tmp.j3_1 = tmp_0 + (value == null ? 'null' : value);
  return this;
};
protoOf(StringBuilder).toString = function () {
  return this.j3_1;
};
function uppercaseChar(_this__u8e3s4) {
  // Inline function 'kotlin.text.uppercase' call
  // Inline function 'kotlin.js.asDynamic' call
  // Inline function 'kotlin.js.unsafeCast' call
  var uppercase = toString(_this__u8e3s4).toUpperCase();
  return uppercase.length > 1 ? _this__u8e3s4 : charCodeAt(uppercase, 0);
}
var STRING_CASE_INSENSITIVE_ORDER;
function substring(_this__u8e3s4, startIndex, endIndex) {
  _init_properties_stringJs_kt__bg7zye();
  // Inline function 'kotlin.js.asDynamic' call
  return _this__u8e3s4.substring(startIndex, endIndex);
}
function substring_0(_this__u8e3s4, startIndex) {
  _init_properties_stringJs_kt__bg7zye();
  // Inline function 'kotlin.js.asDynamic' call
  return _this__u8e3s4.substring(startIndex);
}
function compareTo_0(_this__u8e3s4, other, ignoreCase) {
  ignoreCase = ignoreCase === VOID ? false : ignoreCase;
  _init_properties_stringJs_kt__bg7zye();
  if (ignoreCase) {
    var n1 = _this__u8e3s4.length;
    var n2 = other.length;
    // Inline function 'kotlin.comparisons.minOf' call
    var min = Math.min(n1, n2);
    if (min === 0)
      return n1 - n2 | 0;
    var inductionVariable = 0;
    if (inductionVariable < min)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var thisChar = charCodeAt(_this__u8e3s4, index);
        var otherChar = charCodeAt(other, index);
        if (!(thisChar === otherChar)) {
          thisChar = uppercaseChar(thisChar);
          otherChar = uppercaseChar(otherChar);
          if (!(thisChar === otherChar)) {
            // Inline function 'kotlin.text.lowercaseChar' call
            // Inline function 'kotlin.text.lowercase' call
            var this_0 = thisChar;
            // Inline function 'kotlin.js.asDynamic' call
            // Inline function 'kotlin.js.unsafeCast' call
            var tmp$ret$3 = toString(this_0).toLowerCase();
            thisChar = charCodeAt(tmp$ret$3, 0);
            // Inline function 'kotlin.text.lowercaseChar' call
            // Inline function 'kotlin.text.lowercase' call
            var this_1 = otherChar;
            // Inline function 'kotlin.js.asDynamic' call
            // Inline function 'kotlin.js.unsafeCast' call
            var tmp$ret$7 = toString(this_1).toLowerCase();
            otherChar = charCodeAt(tmp$ret$7, 0);
            if (!(thisChar === otherChar)) {
              return Char__compareTo_impl_ypi4mb(thisChar, otherChar);
            }
          }
        }
      }
       while (inductionVariable < min);
    return n1 - n2 | 0;
  } else {
    return compareTo(_this__u8e3s4, other);
  }
}
function sam$kotlin_Comparator$0(function_0) {
  this.o6_1 = function_0;
}
protoOf(sam$kotlin_Comparator$0).p6 = function (a, b) {
  return this.o6_1(a, b);
};
protoOf(sam$kotlin_Comparator$0).compare = function (a, b) {
  return this.p6(a, b);
};
protoOf(sam$kotlin_Comparator$0).y = function () {
  return this.o6_1;
};
protoOf(sam$kotlin_Comparator$0).equals = function (other) {
  var tmp;
  if (!(other == null) ? isInterface(other, Comparator) : false) {
    var tmp_0;
    if (!(other == null) ? isInterface(other, FunctionAdapter) : false) {
      tmp_0 = equals(this.y(), other.y());
    } else {
      tmp_0 = false;
    }
    tmp = tmp_0;
  } else {
    tmp = false;
  }
  return tmp;
};
protoOf(sam$kotlin_Comparator$0).hashCode = function () {
  return hashCode_0(this.y());
};
function STRING_CASE_INSENSITIVE_ORDER$lambda(a, b) {
  _init_properties_stringJs_kt__bg7zye();
  return compareTo_0(a, b, true);
}
var properties_initialized_stringJs_kt_nta8o4;
function _init_properties_stringJs_kt__bg7zye() {
  if (!properties_initialized_stringJs_kt_nta8o4) {
    properties_initialized_stringJs_kt_nta8o4 = true;
    var tmp = STRING_CASE_INSENSITIVE_ORDER$lambda;
    STRING_CASE_INSENSITIVE_ORDER = new sam$kotlin_Comparator$0(tmp);
  }
}
function addSuppressed(_this__u8e3s4, exception) {
  if (!(_this__u8e3s4 === exception)) {
    // Inline function 'kotlin.js.asDynamic' call
    // Inline function 'kotlin.js.unsafeCast' call
    var suppressed = _this__u8e3s4._suppressed;
    if (suppressed == null) {
      // Inline function 'kotlin.js.asDynamic' call
      _this__u8e3s4._suppressed = mutableListOf([exception]);
    } else {
      suppressed.b1(exception);
    }
  }
}
function AbstractCollection$toString$lambda(this$0) {
  return function (it) {
    return it === this$0 ? '(this Collection)' : toString_0(it);
  };
}
function AbstractCollection() {
}
protoOf(AbstractCollection).h = function (element) {
  var tmp$ret$0;
  $l$block_0: {
    // Inline function 'kotlin.collections.any' call
    var tmp;
    if (isInterface(this, Collection)) {
      tmp = this.g();
    } else {
      tmp = false;
    }
    if (tmp) {
      tmp$ret$0 = false;
      break $l$block_0;
    }
    var _iterator__ex2g4s = this.c();
    while (_iterator__ex2g4s.d()) {
      var element_0 = _iterator__ex2g4s.e();
      if (equals(element_0, element)) {
        tmp$ret$0 = true;
        break $l$block_0;
      }
    }
    tmp$ret$0 = false;
  }
  return tmp$ret$0;
};
protoOf(AbstractCollection).j = function (elements) {
  var tmp$ret$0;
  $l$block_0: {
    // Inline function 'kotlin.collections.all' call
    var tmp;
    if (isInterface(elements, Collection)) {
      tmp = elements.g();
    } else {
      tmp = false;
    }
    if (tmp) {
      tmp$ret$0 = true;
      break $l$block_0;
    }
    var _iterator__ex2g4s = elements.c();
    while (_iterator__ex2g4s.d()) {
      var element = _iterator__ex2g4s.e();
      if (!this.h(element)) {
        tmp$ret$0 = false;
        break $l$block_0;
      }
    }
    tmp$ret$0 = true;
  }
  return tmp$ret$0;
};
protoOf(AbstractCollection).g = function () {
  return this.f() === 0;
};
protoOf(AbstractCollection).toString = function () {
  return joinToString_0(this, ', ', '[', ']', VOID, VOID, AbstractCollection$toString$lambda(this));
};
protoOf(AbstractCollection).toArray = function () {
  return collectionToArray(this);
};
function Companion_3() {
  this.z_1 = 2147483639;
}
protoOf(Companion_3).s1 = function (index, size) {
  if (index < 0 || index >= size) {
    throw IndexOutOfBoundsException_init_$Create$_0('index: ' + index + ', size: ' + size);
  }
};
protoOf(Companion_3).t1 = function (index, size) {
  if (index < 0 || index > size) {
    throw IndexOutOfBoundsException_init_$Create$_0('index: ' + index + ', size: ' + size);
  }
};
protoOf(Companion_3).a1 = function (fromIndex, toIndex, size) {
  if (fromIndex < 0 || toIndex > size) {
    throw IndexOutOfBoundsException_init_$Create$_0('fromIndex: ' + fromIndex + ', toIndex: ' + toIndex + ', size: ' + size);
  }
  if (fromIndex > toIndex) {
    throw IllegalArgumentException_init_$Create$_0('fromIndex: ' + fromIndex + ' > toIndex: ' + toIndex);
  }
};
protoOf(Companion_3).m2 = function (oldCapacity, minCapacity) {
  var newCapacity = oldCapacity + (oldCapacity >> 1) | 0;
  if ((newCapacity - minCapacity | 0) < 0)
    newCapacity = minCapacity;
  if ((newCapacity - 2147483639 | 0) > 0)
    newCapacity = minCapacity > 2147483639 ? 2147483647 : 2147483639;
  return newCapacity;
};
protoOf(Companion_3).l1 = function (c) {
  var hashCode = 1;
  var _iterator__ex2g4s = c.c();
  while (_iterator__ex2g4s.d()) {
    var e = _iterator__ex2g4s.e();
    var tmp = imul_0(31, hashCode);
    var tmp1_elvis_lhs = e == null ? null : hashCode_0(e);
    hashCode = tmp + (tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs) | 0;
  }
  return hashCode;
};
protoOf(Companion_3).k1 = function (c, other) {
  if (!(c.f() === other.f()))
    return false;
  var otherIterator = other.c();
  var _iterator__ex2g4s = c.c();
  while (_iterator__ex2g4s.d()) {
    var elem = _iterator__ex2g4s.e();
    var elemOther = otherIterator.e();
    if (!equals(elem, elemOther)) {
      return false;
    }
  }
  return true;
};
var Companion_instance_3;
function Companion_getInstance_3() {
  return Companion_instance_3;
}
function Companion_4() {
}
protoOf(Companion_4).n1 = function (c) {
  var hashCode = 0;
  var _iterator__ex2g4s = c.c();
  while (_iterator__ex2g4s.d()) {
    var element = _iterator__ex2g4s.e();
    var tmp = hashCode;
    var tmp1_elvis_lhs = element == null ? null : hashCode_0(element);
    hashCode = tmp + (tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs) | 0;
  }
  return hashCode;
};
protoOf(Companion_4).m1 = function (c, other) {
  if (!(c.f() === other.f()))
    return false;
  return c.j(other);
};
var Companion_instance_4;
function Companion_getInstance_4() {
  return Companion_instance_4;
}
function ArrayDeque_init_$Init$($this) {
  AbstractMutableList.call($this);
  ArrayDeque.call($this);
  $this.s6_1 = Companion_getInstance_5().u6_1;
  return $this;
}
function ArrayDeque_init_$Create$() {
  return ArrayDeque_init_$Init$(objectCreate(protoOf(ArrayDeque)));
}
function ensureCapacity_0($this, minCapacity) {
  if (minCapacity < 0)
    throw IllegalStateException_init_$Create$_0('Deque is too big.');
  if (minCapacity <= $this.s6_1.length)
    return Unit_instance;
  if ($this.s6_1 === Companion_getInstance_5().u6_1) {
    var tmp = $this;
    // Inline function 'kotlin.arrayOfNulls' call
    var size = coerceAtLeast(minCapacity, 10);
    tmp.s6_1 = Array(size);
    return Unit_instance;
  }
  var newCapacity = Companion_instance_3.m2($this.s6_1.length, minCapacity);
  copyElements($this, newCapacity);
}
function copyElements($this, newCapacity) {
  // Inline function 'kotlin.arrayOfNulls' call
  var newElements = Array(newCapacity);
  var tmp0 = $this.s6_1;
  var tmp6 = $this.r6_1;
  // Inline function 'kotlin.collections.copyInto' call
  var endIndex = $this.s6_1.length;
  arrayCopy(tmp0, newElements, 0, tmp6, endIndex);
  var tmp0_0 = $this.s6_1;
  var tmp4 = $this.s6_1.length - $this.r6_1 | 0;
  // Inline function 'kotlin.collections.copyInto' call
  var endIndex_0 = $this.r6_1;
  arrayCopy(tmp0_0, newElements, tmp4, 0, endIndex_0);
  $this.r6_1 = 0;
  $this.s6_1 = newElements;
}
function positiveMod($this, index) {
  return index >= $this.s6_1.length ? index - $this.s6_1.length | 0 : index;
}
function incremented($this, index) {
  return index === get_lastIndex($this.s6_1) ? 0 : index + 1 | 0;
}
function decremented($this, index) {
  return index === 0 ? get_lastIndex($this.s6_1) : index - 1 | 0;
}
function registerModification_0($this) {
  $this.g1_1 = $this.g1_1 + 1 | 0;
}
function Companion_5() {
  Companion_instance_5 = this;
  var tmp = this;
  // Inline function 'kotlin.emptyArray' call
  tmp.u6_1 = [];
  this.v6_1 = 10;
}
var Companion_instance_5;
function Companion_getInstance_5() {
  if (Companion_instance_5 == null)
    new Companion_5();
  return Companion_instance_5;
}
protoOf(ArrayDeque).f = function () {
  return this.t6_1;
};
protoOf(ArrayDeque).g = function () {
  return this.t6_1 === 0;
};
protoOf(ArrayDeque).w6 = function (element) {
  registerModification_0(this);
  ensureCapacity_0(this, this.t6_1 + 1 | 0);
  this.r6_1 = decremented(this, this.r6_1);
  this.s6_1[this.r6_1] = element;
  this.t6_1 = this.t6_1 + 1 | 0;
};
protoOf(ArrayDeque).x6 = function (element) {
  registerModification_0(this);
  ensureCapacity_0(this, this.t6_1 + 1 | 0);
  var tmp = this.s6_1;
  // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
  var index = this.t6_1;
  tmp[positiveMod(this, this.r6_1 + index | 0)] = element;
  this.t6_1 = this.t6_1 + 1 | 0;
};
protoOf(ArrayDeque).y6 = function () {
  if (this.g())
    throw NoSuchElementException_init_$Create$_0('ArrayDeque is empty.');
  registerModification_0(this);
  // Inline function 'kotlin.collections.ArrayDeque.internalGet' call
  var internalIndex = this.r6_1;
  var tmp = this.s6_1[internalIndex];
  var element = (tmp == null ? true : !(tmp == null)) ? tmp : THROW_CCE();
  this.s6_1[this.r6_1] = null;
  this.r6_1 = incremented(this, this.r6_1);
  this.t6_1 = this.t6_1 - 1 | 0;
  return element;
};
protoOf(ArrayDeque).z6 = function () {
  return this.g() ? null : this.y6();
};
protoOf(ArrayDeque).a7 = function () {
  if (this.g())
    throw NoSuchElementException_init_$Create$_0('ArrayDeque is empty.');
  registerModification_0(this);
  // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
  var index = get_lastIndex_0(this);
  var internalLastIndex = positiveMod(this, this.r6_1 + index | 0);
  // Inline function 'kotlin.collections.ArrayDeque.internalGet' call
  var tmp = this.s6_1[internalLastIndex];
  var element = (tmp == null ? true : !(tmp == null)) ? tmp : THROW_CCE();
  this.s6_1[internalLastIndex] = null;
  this.t6_1 = this.t6_1 - 1 | 0;
  return element;
};
protoOf(ArrayDeque).b1 = function (element) {
  this.x6(element);
  return true;
};
protoOf(ArrayDeque).h1 = function (index, element) {
  Companion_instance_3.t1(index, this.t6_1);
  if (index === this.t6_1) {
    this.x6(element);
    return Unit_instance;
  } else if (index === 0) {
    this.w6(element);
    return Unit_instance;
  }
  registerModification_0(this);
  ensureCapacity_0(this, this.t6_1 + 1 | 0);
  // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
  var internalIndex = positiveMod(this, this.r6_1 + index | 0);
  if (index < (this.t6_1 + 1 | 0) >> 1) {
    var decrementedInternalIndex = decremented(this, internalIndex);
    var decrementedHead = decremented(this, this.r6_1);
    if (decrementedInternalIndex >= this.r6_1) {
      this.s6_1[decrementedHead] = this.s6_1[this.r6_1];
      var tmp0 = this.s6_1;
      var tmp2 = this.s6_1;
      var tmp4 = this.r6_1;
      var tmp6 = this.r6_1 + 1 | 0;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex = decrementedInternalIndex + 1 | 0;
      arrayCopy(tmp0, tmp2, tmp4, tmp6, endIndex);
    } else {
      var tmp0_0 = this.s6_1;
      var tmp2_0 = this.s6_1;
      var tmp4_0 = this.r6_1 - 1 | 0;
      var tmp6_0 = this.r6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex_0 = this.s6_1.length;
      arrayCopy(tmp0_0, tmp2_0, tmp4_0, tmp6_0, endIndex_0);
      this.s6_1[this.s6_1.length - 1 | 0] = this.s6_1[0];
      var tmp0_1 = this.s6_1;
      var tmp2_1 = this.s6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex_1 = decrementedInternalIndex + 1 | 0;
      arrayCopy(tmp0_1, tmp2_1, 0, 1, endIndex_1);
    }
    this.s6_1[decrementedInternalIndex] = element;
    this.r6_1 = decrementedHead;
  } else {
    // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
    var index_0 = this.t6_1;
    var tail = positiveMod(this, this.r6_1 + index_0 | 0);
    if (internalIndex < tail) {
      var tmp0_2 = this.s6_1;
      var tmp2_2 = this.s6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var destinationOffset = internalIndex + 1 | 0;
      arrayCopy(tmp0_2, tmp2_2, destinationOffset, internalIndex, tail);
    } else {
      var tmp0_3 = this.s6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var destination = this.s6_1;
      arrayCopy(tmp0_3, destination, 1, 0, tail);
      this.s6_1[0] = this.s6_1[this.s6_1.length - 1 | 0];
      var tmp0_4 = this.s6_1;
      var tmp2_3 = this.s6_1;
      var tmp4_1 = internalIndex + 1 | 0;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex_2 = this.s6_1.length - 1 | 0;
      arrayCopy(tmp0_4, tmp2_3, tmp4_1, internalIndex, endIndex_2);
    }
    this.s6_1[internalIndex] = element;
  }
  this.t6_1 = this.t6_1 + 1 | 0;
};
protoOf(ArrayDeque).i = function (index) {
  Companion_instance_3.s1(index, this.t6_1);
  // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
  // Inline function 'kotlin.collections.ArrayDeque.internalGet' call
  var internalIndex = positiveMod(this, this.r6_1 + index | 0);
  var tmp = this.s6_1[internalIndex];
  return (tmp == null ? true : !(tmp == null)) ? tmp : THROW_CCE();
};
protoOf(ArrayDeque).h = function (element) {
  return !(this.j1(element) === -1);
};
protoOf(ArrayDeque).j1 = function (element) {
  // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
  var index = this.t6_1;
  var tail = positiveMod(this, this.r6_1 + index | 0);
  if (this.r6_1 < tail) {
    var inductionVariable = this.r6_1;
    if (inductionVariable < tail)
      do {
        var index_0 = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (equals(element, this.s6_1[index_0]))
          return index_0 - this.r6_1 | 0;
      }
       while (inductionVariable < tail);
  } else if (this.r6_1 >= tail) {
    var inductionVariable_0 = this.r6_1;
    var last = this.s6_1.length;
    if (inductionVariable_0 < last)
      do {
        var index_1 = inductionVariable_0;
        inductionVariable_0 = inductionVariable_0 + 1 | 0;
        if (equals(element, this.s6_1[index_1]))
          return index_1 - this.r6_1 | 0;
      }
       while (inductionVariable_0 < last);
    var inductionVariable_1 = 0;
    if (inductionVariable_1 < tail)
      do {
        var index_2 = inductionVariable_1;
        inductionVariable_1 = inductionVariable_1 + 1 | 0;
        if (equals(element, this.s6_1[index_2]))
          return (index_2 + this.s6_1.length | 0) - this.r6_1 | 0;
      }
       while (inductionVariable_1 < tail);
  }
  return -1;
};
protoOf(ArrayDeque).i1 = function (index) {
  Companion_instance_3.s1(index, this.t6_1);
  if (index === get_lastIndex_0(this)) {
    return this.a7();
  } else if (index === 0) {
    return this.y6();
  }
  registerModification_0(this);
  // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
  var internalIndex = positiveMod(this, this.r6_1 + index | 0);
  // Inline function 'kotlin.collections.ArrayDeque.internalGet' call
  var tmp = this.s6_1[internalIndex];
  var element = (tmp == null ? true : !(tmp == null)) ? tmp : THROW_CCE();
  if (index < this.t6_1 >> 1) {
    if (internalIndex >= this.r6_1) {
      var tmp0 = this.s6_1;
      var tmp2 = this.s6_1;
      var tmp4 = this.r6_1 + 1 | 0;
      // Inline function 'kotlin.collections.copyInto' call
      var startIndex = this.r6_1;
      arrayCopy(tmp0, tmp2, tmp4, startIndex, internalIndex);
    } else {
      var tmp0_0 = this.s6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var destination = this.s6_1;
      arrayCopy(tmp0_0, destination, 1, 0, internalIndex);
      this.s6_1[0] = this.s6_1[this.s6_1.length - 1 | 0];
      var tmp0_1 = this.s6_1;
      var tmp2_0 = this.s6_1;
      var tmp4_0 = this.r6_1 + 1 | 0;
      var tmp6 = this.r6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex = this.s6_1.length - 1 | 0;
      arrayCopy(tmp0_1, tmp2_0, tmp4_0, tmp6, endIndex);
    }
    this.s6_1[this.r6_1] = null;
    this.r6_1 = incremented(this, this.r6_1);
  } else {
    // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
    var index_0 = get_lastIndex_0(this);
    var internalLastIndex = positiveMod(this, this.r6_1 + index_0 | 0);
    if (internalIndex <= internalLastIndex) {
      var tmp0_2 = this.s6_1;
      var tmp2_1 = this.s6_1;
      var tmp6_0 = internalIndex + 1 | 0;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex_0 = internalLastIndex + 1 | 0;
      arrayCopy(tmp0_2, tmp2_1, internalIndex, tmp6_0, endIndex_0);
    } else {
      var tmp0_3 = this.s6_1;
      var tmp2_2 = this.s6_1;
      var tmp6_1 = internalIndex + 1 | 0;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex_1 = this.s6_1.length;
      arrayCopy(tmp0_3, tmp2_2, internalIndex, tmp6_1, endIndex_1);
      this.s6_1[this.s6_1.length - 1 | 0] = this.s6_1[0];
      var tmp0_4 = this.s6_1;
      var tmp2_3 = this.s6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex_2 = internalLastIndex + 1 | 0;
      arrayCopy(tmp0_4, tmp2_3, 0, 1, endIndex_2);
    }
    this.s6_1[internalLastIndex] = null;
  }
  this.t6_1 = this.t6_1 - 1 | 0;
  return element;
};
protoOf(ArrayDeque).b7 = function (array) {
  var tmp = array.length >= this.t6_1 ? array : arrayOfNulls(array, this.t6_1);
  var dest = isArray(tmp) ? tmp : THROW_CCE();
  // Inline function 'kotlin.collections.ArrayDeque.internalIndex' call
  var index = this.t6_1;
  var tail = positiveMod(this, this.r6_1 + index | 0);
  if (this.r6_1 < tail) {
    var tmp0 = this.s6_1;
    // Inline function 'kotlin.collections.copyInto' call
    var startIndex = this.r6_1;
    arrayCopy(tmp0, dest, 0, startIndex, tail);
  } else {
    // Inline function 'kotlin.collections.isNotEmpty' call
    if (!this.g()) {
      var tmp0_0 = this.s6_1;
      var tmp6 = this.r6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var endIndex = this.s6_1.length;
      arrayCopy(tmp0_0, dest, 0, tmp6, endIndex);
      var tmp0_1 = this.s6_1;
      // Inline function 'kotlin.collections.copyInto' call
      var destinationOffset = this.s6_1.length - this.r6_1 | 0;
      arrayCopy(tmp0_1, dest, destinationOffset, 0, tail);
    }
  }
  var tmp_0 = terminateCollectionToArray(this.t6_1, dest);
  return isArray(tmp_0) ? tmp_0 : THROW_CCE();
};
protoOf(ArrayDeque).u1 = function () {
  // Inline function 'kotlin.arrayOfNulls' call
  var size = this.t6_1;
  var tmp$ret$0 = Array(size);
  return this.b7(tmp$ret$0);
};
protoOf(ArrayDeque).toArray = function () {
  return this.u1();
};
function ArrayDeque() {
  Companion_getInstance_5();
  this.r6_1 = 0;
  this.t6_1 = 0;
}
function collectionToArrayCommonImpl(collection) {
  if (collection.g()) {
    // Inline function 'kotlin.emptyArray' call
    return [];
  }
  // Inline function 'kotlin.arrayOfNulls' call
  var size = collection.f();
  var destination = Array(size);
  var iterator = collection.c();
  var index = 0;
  while (iterator.d()) {
    var _unary__edvuaz = index;
    index = _unary__edvuaz + 1 | 0;
    destination[_unary__edvuaz] = iterator.e();
  }
  return destination;
}
function mutableListOf(elements) {
  var tmp;
  if (elements.length === 0) {
    tmp = ArrayList_init_$Create$();
  } else {
    // Inline function 'kotlin.collections.asArrayList' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp = new ArrayList(elements);
  }
  return tmp;
}
function get_lastIndex_0(_this__u8e3s4) {
  return _this__u8e3s4.f() - 1 | 0;
}
function removeFirstOrNull(_this__u8e3s4) {
  return _this__u8e3s4.g() ? null : _this__u8e3s4.i1(0);
}
function Continuation() {
}
function startCoroutine(_this__u8e3s4, receiver, completion) {
  // Inline function 'kotlin.coroutines.resume' call
  var this_0 = intercepted(createCoroutineUnintercepted(_this__u8e3s4, receiver, completion));
  // Inline function 'kotlin.Companion.success' call
  var tmp$ret$0 = _Result___init__impl__xyqfz8(Unit_instance);
  this_0.k4(tmp$ret$0);
}
function Key() {
}
var Key_instance;
function Key_getInstance() {
  return Key_instance;
}
function ContinuationInterceptor() {
}
function Element() {
}
function CoroutineContext$plus$lambda(acc, element) {
  var removed = acc.g7(element.k());
  var tmp;
  if (removed === EmptyCoroutineContext_getInstance()) {
    tmp = element;
  } else {
    var interceptor = removed.m4(Key_instance);
    var tmp_0;
    if (interceptor == null) {
      tmp_0 = new CombinedContext(removed, element);
    } else {
      var left = removed.g7(Key_instance);
      tmp_0 = left === EmptyCoroutineContext_getInstance() ? new CombinedContext(element, interceptor) : new CombinedContext(new CombinedContext(left, element), interceptor);
    }
    tmp = tmp_0;
  }
  return tmp;
}
function CoroutineContext() {
}
function EmptyCoroutineContext() {
  EmptyCoroutineContext_instance = this;
  this.j7_1 = new Long(0, 0);
}
protoOf(EmptyCoroutineContext).m4 = function (key) {
  return null;
};
protoOf(EmptyCoroutineContext).h7 = function (initial, operation) {
  return initial;
};
protoOf(EmptyCoroutineContext).i7 = function (context) {
  return context;
};
protoOf(EmptyCoroutineContext).g7 = function (key) {
  return this;
};
protoOf(EmptyCoroutineContext).hashCode = function () {
  return 0;
};
protoOf(EmptyCoroutineContext).toString = function () {
  return 'EmptyCoroutineContext';
};
var EmptyCoroutineContext_instance;
function EmptyCoroutineContext_getInstance() {
  if (EmptyCoroutineContext_instance == null)
    new EmptyCoroutineContext();
  return EmptyCoroutineContext_instance;
}
function size($this) {
  var cur = $this;
  var size = 2;
  while (true) {
    var tmp = cur.k7_1;
    var tmp0_elvis_lhs = tmp instanceof CombinedContext ? tmp : null;
    var tmp_0;
    if (tmp0_elvis_lhs == null) {
      return size;
    } else {
      tmp_0 = tmp0_elvis_lhs;
    }
    cur = tmp_0;
    size = size + 1 | 0;
  }
}
function contains($this, element) {
  return equals($this.m4(element.k()), element);
}
function containsAll($this, context) {
  var cur = context;
  while (true) {
    if (!contains($this, cur.l7_1))
      return false;
    var next = cur.k7_1;
    if (next instanceof CombinedContext) {
      cur = next;
    } else {
      return contains($this, isInterface(next, Element) ? next : THROW_CCE());
    }
  }
}
function CombinedContext$toString$lambda(acc, element) {
  var tmp;
  // Inline function 'kotlin.text.isEmpty' call
  if (charSequenceLength(acc) === 0) {
    tmp = toString_1(element);
  } else {
    tmp = acc + ', ' + toString_1(element);
  }
  return tmp;
}
function CombinedContext(left, element) {
  this.k7_1 = left;
  this.l7_1 = element;
}
protoOf(CombinedContext).m4 = function (key) {
  var cur = this;
  while (true) {
    var tmp0_safe_receiver = cur.l7_1.m4(key);
    if (tmp0_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      return tmp0_safe_receiver;
    }
    var next = cur.k7_1;
    if (next instanceof CombinedContext) {
      cur = next;
    } else {
      return next.m4(key);
    }
  }
};
protoOf(CombinedContext).h7 = function (initial, operation) {
  return operation(this.k7_1.h7(initial, operation), this.l7_1);
};
protoOf(CombinedContext).g7 = function (key) {
  if (this.l7_1.m4(key) == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    return this.k7_1;
  }
  var newLeft = this.k7_1.g7(key);
  return newLeft === this.k7_1 ? this : newLeft === EmptyCoroutineContext_getInstance() ? this.l7_1 : new CombinedContext(newLeft, this.l7_1);
};
protoOf(CombinedContext).equals = function (other) {
  var tmp;
  if (this === other) {
    tmp = true;
  } else {
    var tmp_0;
    var tmp_1;
    if (other instanceof CombinedContext) {
      tmp_1 = size(other) === size(this);
    } else {
      tmp_1 = false;
    }
    if (tmp_1) {
      tmp_0 = containsAll(other, this);
    } else {
      tmp_0 = false;
    }
    tmp = tmp_0;
  }
  return tmp;
};
protoOf(CombinedContext).hashCode = function () {
  return hashCode_0(this.k7_1) + hashCode_0(this.l7_1) | 0;
};
protoOf(CombinedContext).toString = function () {
  return '[' + this.h7('', CombinedContext$toString$lambda) + ']';
};
function AbstractCoroutineContextKey(baseKey, safeCast) {
  this.c7_1 = safeCast;
  var tmp = this;
  var tmp_0;
  if (baseKey instanceof AbstractCoroutineContextKey) {
    tmp_0 = baseKey.d7_1;
  } else {
    tmp_0 = baseKey;
  }
  tmp.d7_1 = tmp_0;
}
protoOf(AbstractCoroutineContextKey).e7 = function (element) {
  return this.c7_1(element);
};
protoOf(AbstractCoroutineContextKey).f7 = function (key) {
  return key === this || this.d7_1 === key;
};
function AbstractCoroutineContextElement(key) {
  this.m7_1 = key;
}
protoOf(AbstractCoroutineContextElement).k = function () {
  return this.m7_1;
};
function get_COROUTINE_SUSPENDED() {
  return CoroutineSingletons_COROUTINE_SUSPENDED_getInstance();
}
var CoroutineSingletons_COROUTINE_SUSPENDED_instance;
var CoroutineSingletons_UNDECIDED_instance;
var CoroutineSingletons_RESUMED_instance;
var CoroutineSingletons_entriesInitialized;
function CoroutineSingletons_initEntries() {
  if (CoroutineSingletons_entriesInitialized)
    return Unit_instance;
  CoroutineSingletons_entriesInitialized = true;
  CoroutineSingletons_COROUTINE_SUSPENDED_instance = new CoroutineSingletons('COROUTINE_SUSPENDED', 0);
  CoroutineSingletons_UNDECIDED_instance = new CoroutineSingletons('UNDECIDED', 1);
  CoroutineSingletons_RESUMED_instance = new CoroutineSingletons('RESUMED', 2);
}
function CoroutineSingletons(name, ordinal) {
  Enum.call(this, name, ordinal);
}
function CoroutineSingletons_COROUTINE_SUSPENDED_getInstance() {
  CoroutineSingletons_initEntries();
  return CoroutineSingletons_COROUTINE_SUSPENDED_instance;
}
function appendElement(_this__u8e3s4, element, transform) {
  if (!(transform == null))
    _this__u8e3s4.b(transform(element));
  else {
    if (element == null ? true : isCharSequence(element))
      _this__u8e3s4.b(element);
    else {
      if (element instanceof Char)
        _this__u8e3s4.m3(element.n7_1);
      else {
        _this__u8e3s4.b(toString_1(element));
      }
    }
  }
}
function _Result___init__impl__xyqfz8(value) {
  return value;
}
function _Result___get_value__impl__bjfvqg($this) {
  return $this;
}
function _Result___get_isFailure__impl__jpiriv($this) {
  var tmp = _Result___get_value__impl__bjfvqg($this);
  return tmp instanceof Failure;
}
function Result__exceptionOrNull_impl_p6xea9($this) {
  var tmp;
  if (_Result___get_value__impl__bjfvqg($this) instanceof Failure) {
    tmp = _Result___get_value__impl__bjfvqg($this).o7_1;
  } else {
    tmp = null;
  }
  return tmp;
}
function Companion_6() {
}
var Companion_instance_6;
function Companion_getInstance_6() {
  return Companion_instance_6;
}
function Failure(exception) {
  this.o7_1 = exception;
}
protoOf(Failure).equals = function (other) {
  var tmp;
  if (other instanceof Failure) {
    tmp = equals(this.o7_1, other.o7_1);
  } else {
    tmp = false;
  }
  return tmp;
};
protoOf(Failure).hashCode = function () {
  return hashCode_0(this.o7_1);
};
protoOf(Failure).toString = function () {
  return 'Failure(' + this.o7_1.toString() + ')';
};
function createFailure(exception) {
  return new Failure(exception);
}
function NotImplementedError(message) {
  message = message === VOID ? 'An operation is not implemented.' : message;
  Error_init_$Init$_0(message, this);
  captureStack(this, NotImplementedError);
}
function None() {
  None_instance = this;
  atomicfu$TraceBase.call(this);
}
var None_instance;
function None_getInstance() {
  if (None_instance == null)
    new None();
  return None_instance;
}
function atomicfu$TraceBase() {
}
protoOf(atomicfu$TraceBase).atomicfu$Trace$append$1 = function (event) {
};
protoOf(atomicfu$TraceBase).atomicfu$Trace$append$2 = function (event1, event2) {
};
protoOf(atomicfu$TraceBase).atomicfu$Trace$append$3 = function (event1, event2, event3) {
};
protoOf(atomicfu$TraceBase).atomicfu$Trace$append$4 = function (event1, event2, event3, event4) {
};
function AtomicRef(value) {
  this.kotlinx$atomicfu$value = value;
}
protoOf(AtomicRef).p7 = function (_set____db54di) {
  this.kotlinx$atomicfu$value = _set____db54di;
};
protoOf(AtomicRef).q7 = function () {
  return this.kotlinx$atomicfu$value;
};
protoOf(AtomicRef).atomicfu$compareAndSet = function (expect, update) {
  if (!(this.kotlinx$atomicfu$value === expect))
    return false;
  this.kotlinx$atomicfu$value = update;
  return true;
};
protoOf(AtomicRef).atomicfu$getAndSet = function (value) {
  var oldValue = this.kotlinx$atomicfu$value;
  this.kotlinx$atomicfu$value = value;
  return oldValue;
};
protoOf(AtomicRef).toString = function () {
  return toString_0(this.kotlinx$atomicfu$value);
};
function atomic$ref$1(initial) {
  return atomic$ref$(initial, None_getInstance());
}
function AtomicBoolean(value) {
  this.kotlinx$atomicfu$value = value;
}
protoOf(AtomicBoolean).r7 = function (_set____db54di) {
  this.kotlinx$atomicfu$value = _set____db54di;
};
protoOf(AtomicBoolean).q7 = function () {
  return this.kotlinx$atomicfu$value;
};
protoOf(AtomicBoolean).atomicfu$compareAndSet = function (expect, update) {
  if (!(this.kotlinx$atomicfu$value === expect))
    return false;
  this.kotlinx$atomicfu$value = update;
  return true;
};
protoOf(AtomicBoolean).atomicfu$getAndSet = function (value) {
  var oldValue = this.kotlinx$atomicfu$value;
  this.kotlinx$atomicfu$value = value;
  return oldValue;
};
protoOf(AtomicBoolean).toString = function () {
  return this.kotlinx$atomicfu$value.toString();
};
function atomic$boolean$1(initial) {
  return atomic$boolean$(initial, None_getInstance());
}
function AtomicInt(value) {
  this.kotlinx$atomicfu$value = value;
}
protoOf(AtomicInt).s7 = function (_set____db54di) {
  this.kotlinx$atomicfu$value = _set____db54di;
};
protoOf(AtomicInt).q7 = function () {
  return this.kotlinx$atomicfu$value;
};
protoOf(AtomicInt).atomicfu$compareAndSet = function (expect, update) {
  if (!(this.kotlinx$atomicfu$value === expect))
    return false;
  this.kotlinx$atomicfu$value = update;
  return true;
};
protoOf(AtomicInt).atomicfu$getAndSet = function (value) {
  var oldValue = this.kotlinx$atomicfu$value;
  this.kotlinx$atomicfu$value = value;
  return oldValue;
};
protoOf(AtomicInt).atomicfu$getAndIncrement = function () {
  var tmp1 = this.kotlinx$atomicfu$value;
  this.kotlinx$atomicfu$value = tmp1 + 1 | 0;
  return tmp1;
};
protoOf(AtomicInt).atomicfu$getAndDecrement = function () {
  var tmp1 = this.kotlinx$atomicfu$value;
  this.kotlinx$atomicfu$value = tmp1 - 1 | 0;
  return tmp1;
};
protoOf(AtomicInt).atomicfu$getAndAdd = function (delta) {
  var oldValue = this.kotlinx$atomicfu$value;
  this.kotlinx$atomicfu$value = this.kotlinx$atomicfu$value + delta | 0;
  return oldValue;
};
protoOf(AtomicInt).atomicfu$addAndGet = function (delta) {
  this.kotlinx$atomicfu$value = this.kotlinx$atomicfu$value + delta | 0;
  return this.kotlinx$atomicfu$value;
};
protoOf(AtomicInt).atomicfu$incrementAndGet = function () {
  this.kotlinx$atomicfu$value = this.kotlinx$atomicfu$value + 1 | 0;
  return this.kotlinx$atomicfu$value;
};
protoOf(AtomicInt).atomicfu$decrementAndGet = function () {
  this.kotlinx$atomicfu$value = this.kotlinx$atomicfu$value - 1 | 0;
  return this.kotlinx$atomicfu$value;
};
protoOf(AtomicInt).toString = function () {
  return this.kotlinx$atomicfu$value.toString();
};
function atomic$int$1(initial) {
  return atomic$int$(initial, None_getInstance());
}
function atomic$ref$(initial, trace) {
  trace = trace === VOID ? None_getInstance() : trace;
  return new AtomicRef(initial);
}
function atomic$boolean$(initial, trace) {
  trace = trace === VOID ? None_getInstance() : trace;
  return new AtomicBoolean(initial);
}
function atomic$int$(initial, trace) {
  trace = trace === VOID ? None_getInstance() : trace;
  return new AtomicInt(initial);
}
function AbstractCoroutine(parentContext, initParentJob, active) {
  JobSupport.call(this, active);
  if (initParentJob) {
    this.v7(parentContext.m4(Key_instance_2));
  }
  this.y7_1 = parentContext.i7(this);
}
protoOf(AbstractCoroutine).f4 = function () {
  return this.y7_1;
};
protoOf(AbstractCoroutine).z7 = function () {
  return this.y7_1;
};
protoOf(AbstractCoroutine).a8 = function () {
  return protoOf(JobSupport).a8.call(this);
};
protoOf(AbstractCoroutine).b8 = function (value) {
};
protoOf(AbstractCoroutine).c8 = function (cause, handled) {
};
protoOf(AbstractCoroutine).d8 = function () {
  return get_classSimpleName(this) + ' was cancelled';
};
protoOf(AbstractCoroutine).e8 = function (state) {
  if (state instanceof CompletedExceptionally) {
    this.c8(state.f8_1, state.h8());
  } else {
    this.b8((state == null ? true : !(state == null)) ? state : THROW_CCE());
  }
};
protoOf(AbstractCoroutine).k4 = function (result) {
  var state = this.i8(toState_0(result));
  if (state === get_COMPLETING_WAITING_CHILDREN())
    return Unit_instance;
  this.j8(state);
};
protoOf(AbstractCoroutine).j8 = function (state) {
  return this.k8(state);
};
protoOf(AbstractCoroutine).l8 = function (exception) {
  handleCoroutineException(this.y7_1, exception);
};
protoOf(AbstractCoroutine).m8 = function () {
  var tmp0_elvis_lhs = get_coroutineName(this.y7_1);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return protoOf(JobSupport).m8.call(this);
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var coroutineName = tmp;
  return '"' + coroutineName + '":' + protoOf(JobSupport).m8.call(this);
};
protoOf(AbstractCoroutine).n8 = function (start, receiver, block) {
  start.q8(block, receiver, this);
};
function launch(_this__u8e3s4, context, start, block) {
  context = context === VOID ? EmptyCoroutineContext_getInstance() : context;
  start = start === VOID ? CoroutineStart_DEFAULT_getInstance() : start;
  var newContext = newCoroutineContext(_this__u8e3s4, context);
  var coroutine = start.o9() ? new LazyStandaloneCoroutine(newContext, block) : new StandaloneCoroutine(newContext, true);
  coroutine.n8(start, coroutine, block);
  return coroutine;
}
function StandaloneCoroutine(parentContext, active) {
  AbstractCoroutine.call(this, parentContext, true, active);
}
protoOf(StandaloneCoroutine).m9 = function (exception) {
  handleCoroutineException(this.y7_1, exception);
  return true;
};
function LazyStandaloneCoroutine(parentContext, block) {
  StandaloneCoroutine.call(this, parentContext, false);
  this.v9_1 = createCoroutineUnintercepted(block, this, this);
}
protoOf(LazyStandaloneCoroutine).w8 = function () {
  startCoroutineCancellable(this.v9_1, this);
};
function _get_parentHandle__f8dcex($this) {
  return $this.ba_1.kotlinx$atomicfu$value;
}
function _get_stateDebugRepresentation__bf18u4($this) {
  var tmp0_subject = $this.t8();
  var tmp;
  if (!(tmp0_subject == null) ? isInterface(tmp0_subject, NotCompleted) : false) {
    tmp = 'Active';
  } else {
    if (tmp0_subject instanceof CancelledContinuation) {
      tmp = 'Cancelled';
    } else {
      tmp = 'Completed';
    }
  }
  return tmp;
}
function isReusable($this) {
  var tmp;
  if (get_isReusableMode($this.ja_1)) {
    var tmp_0 = $this.x9_1;
    tmp = (tmp_0 instanceof DispatchedContinuation ? tmp_0 : THROW_CCE()).ia();
  } else {
    tmp = false;
  }
  return tmp;
}
function cancelLater($this, cause) {
  if (!isReusable($this))
    return false;
  var tmp = $this.x9_1;
  var dispatched = tmp instanceof DispatchedContinuation ? tmp : THROW_CCE();
  return dispatched.ka(cause);
}
function callSegmentOnCancellation($this, segment, cause) {
  // Inline function 'kotlinx.coroutines.index' call
  var index = $this.z9_1.kotlinx$atomicfu$value & 536870911;
  // Inline function 'kotlin.check' call
  if (!!(index === 536870911)) {
    var message = 'The index for Segment.onCancellation(..) is broken';
    throw IllegalStateException_init_$Create$_0(toString_1(message));
  }
  // Inline function 'kotlinx.coroutines.CancellableContinuationImpl.callCancelHandlerSafely' call
  try {
    segment.la(index, cause, $this.f4());
  } catch ($p) {
    if ($p instanceof Error) {
      var ex = $p;
      handleCoroutineException($this.f4(), new CompletionHandlerException('Exception in invokeOnCancellation handler for ' + $this.toString(), ex));
    } else {
      throw $p;
    }
  }
}
function trySuspend($this) {
  // Inline function 'kotlinx.atomicfu.loop' call
  var this_0 = $this.z9_1;
  while (true) {
    var cur = this_0.kotlinx$atomicfu$value;
    // Inline function 'kotlinx.coroutines.decision' call
    switch (cur >> 29) {
      case 0:
        // Inline function 'kotlinx.coroutines.index' call

        // Inline function 'kotlinx.coroutines.decisionAndIndex' call

        var tmp$ret$2 = (1 << 29) + (cur & 536870911) | 0;
        if ($this.z9_1.atomicfu$compareAndSet(cur, tmp$ret$2))
          return true;
        break;
      case 2:
        return false;
      default:
        // Inline function 'kotlin.error' call

        var message = 'Already suspended';
        throw IllegalStateException_init_$Create$_0(toString_1(message));
    }
  }
}
function tryResume($this) {
  // Inline function 'kotlinx.atomicfu.loop' call
  var this_0 = $this.z9_1;
  while (true) {
    var cur = this_0.kotlinx$atomicfu$value;
    // Inline function 'kotlinx.coroutines.decision' call
    switch (cur >> 29) {
      case 0:
        // Inline function 'kotlinx.coroutines.index' call

        // Inline function 'kotlinx.coroutines.decisionAndIndex' call

        var tmp$ret$2 = (2 << 29) + (cur & 536870911) | 0;
        if ($this.z9_1.atomicfu$compareAndSet(cur, tmp$ret$2))
          return true;
        break;
      case 1:
        return false;
      default:
        // Inline function 'kotlin.error' call

        var message = 'Already resumed';
        throw IllegalStateException_init_$Create$_0(toString_1(message));
    }
  }
}
function installParentHandle($this) {
  var tmp0_elvis_lhs = $this.f4().m4(Key_instance_2);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return null;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var parent = tmp;
  // Inline function 'kotlinx.coroutines.asHandler' call
  // Inline function 'kotlin.js.asDynamic' call
  var tmp$ret$1 = new ChildContinuation($this);
  var handle = parent.b9(true, VOID, tmp$ret$1);
  $this.ba_1.atomicfu$compareAndSet(null, handle);
  return handle;
}
function dispatchResume($this, mode) {
  if (tryResume($this))
    return Unit_instance;
  dispatch($this, mode);
}
function resumedState($this, state, proposedUpdate, resumeMode, onCancellation, idempotent) {
  var tmp;
  if (proposedUpdate instanceof CompletedExceptionally) {
    // Inline function 'kotlinx.coroutines.assert' call
    // Inline function 'kotlinx.coroutines.assert' call
    tmp = proposedUpdate;
  } else {
    if (!get_isCancellableMode(resumeMode) && idempotent == null) {
      tmp = proposedUpdate;
    } else {
      var tmp_0;
      var tmp_1;
      if (!(onCancellation == null)) {
        tmp_1 = true;
      } else {
        tmp_1 = state instanceof CancelHandler;
      }
      if (tmp_1) {
        tmp_0 = true;
      } else {
        tmp_0 = !(idempotent == null);
      }
      if (tmp_0) {
        tmp = new CompletedContinuation_0(proposedUpdate, state instanceof CancelHandler ? state : null, onCancellation, idempotent);
      } else {
        tmp = proposedUpdate;
      }
    }
  }
  return tmp;
}
function resumeImpl($this, proposedUpdate, resumeMode, onCancellation) {
  // Inline function 'kotlinx.atomicfu.loop' call
  var this_0 = $this.aa_1;
  while (true) {
    var tmp0 = this_0.kotlinx$atomicfu$value;
    $l$block: {
      if (!(tmp0 == null) ? isInterface(tmp0, NotCompleted) : false) {
        var update = resumedState($this, tmp0, proposedUpdate, resumeMode, onCancellation, null);
        if (!$this.aa_1.atomicfu$compareAndSet(tmp0, update)) {
          break $l$block;
        }
        detachChildIfNonResuable($this);
        dispatchResume($this, resumeMode);
        return Unit_instance;
      } else {
        if (tmp0 instanceof CancelledContinuation) {
          if (tmp0.qa()) {
            if (onCancellation == null)
              null;
            else {
              // Inline function 'kotlin.let' call
              $this.ma(onCancellation, tmp0.f8_1);
            }
            return Unit_instance;
          }
        }
      }
      alreadyResumedError($this, proposedUpdate);
    }
  }
}
function resumeImpl$default($this, proposedUpdate, resumeMode, onCancellation, $super) {
  onCancellation = onCancellation === VOID ? null : onCancellation;
  return resumeImpl($this, proposedUpdate, resumeMode, onCancellation);
}
function alreadyResumedError($this, proposedUpdate) {
  // Inline function 'kotlin.error' call
  var message = 'Already resumed, but proposed with update ' + toString_0(proposedUpdate);
  throw IllegalStateException_init_$Create$_0(toString_1(message));
}
function detachChildIfNonResuable($this) {
  if (!isReusable($this)) {
    $this.ra();
  }
}
function CancellableContinuationImpl(delegate, resumeMode) {
  DispatchedTask.call(this, resumeMode);
  this.x9_1 = delegate;
  // Inline function 'kotlinx.coroutines.assert' call
  this.y9_1 = this.x9_1.f4();
  var tmp = this;
  // Inline function 'kotlinx.coroutines.decisionAndIndex' call
  var tmp$ret$1 = (0 << 29) + 536870911 | 0;
  tmp.z9_1 = atomic$int$1(tmp$ret$1);
  this.aa_1 = atomic$ref$1(Active_instance);
  this.ba_1 = atomic$ref$1(null);
}
protoOf(CancellableContinuationImpl).sa = function () {
  return this.x9_1;
};
protoOf(CancellableContinuationImpl).f4 = function () {
  return this.y9_1;
};
protoOf(CancellableContinuationImpl).t8 = function () {
  return this.aa_1.kotlinx$atomicfu$value;
};
protoOf(CancellableContinuationImpl).u8 = function () {
  var tmp = this.t8();
  return !(!(tmp == null) ? isInterface(tmp, NotCompleted) : false);
};
protoOf(CancellableContinuationImpl).ta = function () {
  var tmp0_elvis_lhs = installParentHandle(this);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return Unit_instance;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var handle = tmp;
  if (this.u8()) {
    handle.ua();
    this.ba_1.kotlinx$atomicfu$value = NonDisposableHandle_instance;
  }
};
protoOf(CancellableContinuationImpl).va = function () {
  return this.t8();
};
protoOf(CancellableContinuationImpl).wa = function (takenState, cause) {
  var this_0 = this.aa_1;
  while (true) {
    var state = this_0.kotlinx$atomicfu$value;
    if (!(state == null) ? isInterface(state, NotCompleted) : false) {
      // Inline function 'kotlin.error' call
      var message = 'Not completed';
      throw IllegalStateException_init_$Create$_0(toString_1(message));
    } else {
      if (state instanceof CompletedExceptionally)
        return Unit_instance;
      else {
        if (state instanceof CompletedContinuation_0) {
          // Inline function 'kotlin.check' call
          if (!!state.cb()) {
            var message_0 = 'Must be called at most once';
            throw IllegalStateException_init_$Create$_0(toString_1(message_0));
          }
          var update = state.db(VOID, VOID, VOID, VOID, cause);
          if (this.aa_1.atomicfu$compareAndSet(state, update)) {
            state.eb(this, cause);
            return Unit_instance;
          }
        } else {
          if (this.aa_1.atomicfu$compareAndSet(state, new CompletedContinuation_0(state, VOID, VOID, VOID, cause))) {
            return Unit_instance;
          }
        }
      }
    }
  }
  return Unit_instance;
};
protoOf(CancellableContinuationImpl).fb = function (cause) {
  // Inline function 'kotlinx.atomicfu.loop' call
  var this_0 = this.aa_1;
  while (true) {
    var tmp0 = this_0.kotlinx$atomicfu$value;
    $l$block: {
      if (!(!(tmp0 == null) ? isInterface(tmp0, NotCompleted) : false))
        return false;
      var tmp;
      if (tmp0 instanceof CancelHandler) {
        tmp = true;
      } else {
        tmp = tmp0 instanceof Segment;
      }
      var update = new CancelledContinuation(this, cause, tmp);
      if (!this.aa_1.atomicfu$compareAndSet(tmp0, update)) {
        break $l$block;
      }
      if (tmp0 instanceof CancelHandler) {
        this.gb(tmp0, cause);
      } else {
        if (tmp0 instanceof Segment) {
          callSegmentOnCancellation(this, tmp0, cause);
        }
      }
      detachChildIfNonResuable(this);
      dispatchResume(this, this.ja_1);
      return true;
    }
  }
};
protoOf(CancellableContinuationImpl).hb = function (cause) {
  if (cancelLater(this, cause))
    return Unit_instance;
  this.fb(cause);
  detachChildIfNonResuable(this);
};
protoOf(CancellableContinuationImpl).gb = function (handler, cause) {
  // Inline function 'kotlinx.coroutines.CancellableContinuationImpl.callCancelHandlerSafely' call
  try {
    handler.invoke(cause);
  } catch ($p) {
    if ($p instanceof Error) {
      var ex = $p;
      handleCoroutineException(this.f4(), new CompletionHandlerException('Exception in invokeOnCancellation handler for ' + this.toString(), ex));
    } else {
      throw $p;
    }
  }
  return Unit_instance;
};
protoOf(CancellableContinuationImpl).ma = function (onCancellation, cause) {
  try {
    onCancellation(cause);
  } catch ($p) {
    if ($p instanceof Error) {
      var ex = $p;
      handleCoroutineException(this.f4(), new CompletionHandlerException('Exception in resume onCancellation handler for ' + this.toString(), ex));
    } else {
      throw $p;
    }
  }
};
protoOf(CancellableContinuationImpl).ib = function (parent) {
  return parent.x8();
};
protoOf(CancellableContinuationImpl).jb = function () {
  var isReusable_0 = isReusable(this);
  if (trySuspend(this)) {
    if (_get_parentHandle__f8dcex(this) == null) {
      installParentHandle(this);
    }
    if (isReusable_0) {
      this.kb();
    }
    return get_COROUTINE_SUSPENDED();
  }
  if (isReusable_0) {
    this.kb();
  }
  var state = this.t8();
  if (state instanceof CompletedExceptionally)
    throw recoverStackTrace(state.f8_1, this);
  if (get_isCancellableMode(this.ja_1)) {
    var job = this.f4().m4(Key_instance_2);
    if (!(job == null) && !job.a8()) {
      var cause = job.x8();
      this.wa(state, cause);
      throw recoverStackTrace(cause, this);
    }
  }
  return this.lb(state);
};
protoOf(CancellableContinuationImpl).kb = function () {
  var tmp = this.x9_1;
  var tmp0_safe_receiver = tmp instanceof DispatchedContinuation ? tmp : null;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.mb(this);
  var tmp_0;
  if (tmp1_elvis_lhs == null) {
    return Unit_instance;
  } else {
    tmp_0 = tmp1_elvis_lhs;
  }
  var cancellationCause = tmp_0;
  this.ra();
  this.fb(cancellationCause);
};
protoOf(CancellableContinuationImpl).k4 = function (result) {
  return resumeImpl$default(this, toState(result, this), this.ja_1);
};
protoOf(CancellableContinuationImpl).ra = function () {
  var tmp0_elvis_lhs = _get_parentHandle__f8dcex(this);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return Unit_instance;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var handle = tmp;
  handle.ua();
  this.ba_1.kotlinx$atomicfu$value = NonDisposableHandle_instance;
};
protoOf(CancellableContinuationImpl).lb = function (state) {
  var tmp;
  if (state instanceof CompletedContinuation_0) {
    var tmp_0 = state.xa_1;
    tmp = (tmp_0 == null ? true : !(tmp_0 == null)) ? tmp_0 : THROW_CCE();
  } else {
    tmp = (state == null ? true : !(state == null)) ? state : THROW_CCE();
  }
  return tmp;
};
protoOf(CancellableContinuationImpl).nb = function (state) {
  var tmp0_safe_receiver = protoOf(DispatchedTask).nb.call(this, state);
  var tmp;
  if (tmp0_safe_receiver == null) {
    tmp = null;
  } else {
    // Inline function 'kotlin.let' call
    tmp = recoverStackTrace(tmp0_safe_receiver, this.x9_1);
  }
  return tmp;
};
protoOf(CancellableContinuationImpl).toString = function () {
  return this.ob() + '(' + toDebugString(this.x9_1) + '){' + _get_stateDebugRepresentation__bf18u4(this) + '}@' + get_hexAddress(this);
};
protoOf(CancellableContinuationImpl).ob = function () {
  return 'CancellableContinuation';
};
function NotCompleted() {
}
function CancelHandler() {
}
function Active() {
}
protoOf(Active).toString = function () {
  return 'Active';
};
var Active_instance;
function Active_getInstance() {
  return Active_instance;
}
function CompletedContinuation_0(result, cancelHandler, onCancellation, idempotentResume, cancelCause) {
  cancelHandler = cancelHandler === VOID ? null : cancelHandler;
  onCancellation = onCancellation === VOID ? null : onCancellation;
  idempotentResume = idempotentResume === VOID ? null : idempotentResume;
  cancelCause = cancelCause === VOID ? null : cancelCause;
  this.xa_1 = result;
  this.ya_1 = cancelHandler;
  this.za_1 = onCancellation;
  this.ab_1 = idempotentResume;
  this.bb_1 = cancelCause;
}
protoOf(CompletedContinuation_0).cb = function () {
  return !(this.bb_1 == null);
};
protoOf(CompletedContinuation_0).eb = function (cont, cause) {
  var tmp0_safe_receiver = this.ya_1;
  if (tmp0_safe_receiver == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    cont.gb(tmp0_safe_receiver, cause);
  }
  var tmp1_safe_receiver = this.za_1;
  if (tmp1_safe_receiver == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    cont.ma(tmp1_safe_receiver, cause);
  }
};
protoOf(CompletedContinuation_0).rb = function (result, cancelHandler, onCancellation, idempotentResume, cancelCause) {
  return new CompletedContinuation_0(result, cancelHandler, onCancellation, idempotentResume, cancelCause);
};
protoOf(CompletedContinuation_0).db = function (result, cancelHandler, onCancellation, idempotentResume, cancelCause, $super) {
  result = result === VOID ? this.xa_1 : result;
  cancelHandler = cancelHandler === VOID ? this.ya_1 : cancelHandler;
  onCancellation = onCancellation === VOID ? this.za_1 : onCancellation;
  idempotentResume = idempotentResume === VOID ? this.ab_1 : idempotentResume;
  cancelCause = cancelCause === VOID ? this.bb_1 : cancelCause;
  return $super === VOID ? this.rb(result, cancelHandler, onCancellation, idempotentResume, cancelCause) : $super.rb.call(this, result, cancelHandler, onCancellation, idempotentResume, cancelCause);
};
protoOf(CompletedContinuation_0).toString = function () {
  return 'CompletedContinuation(result=' + toString_0(this.xa_1) + ', cancelHandler=' + toString_0(this.ya_1) + ', onCancellation=' + toString_0(this.za_1) + ', idempotentResume=' + toString_0(this.ab_1) + ', cancelCause=' + toString_0(this.bb_1) + ')';
};
protoOf(CompletedContinuation_0).hashCode = function () {
  var result = this.xa_1 == null ? 0 : hashCode_0(this.xa_1);
  result = imul_0(result, 31) + (this.ya_1 == null ? 0 : hashCode_0(this.ya_1)) | 0;
  result = imul_0(result, 31) + (this.za_1 == null ? 0 : hashCode_0(this.za_1)) | 0;
  result = imul_0(result, 31) + (this.ab_1 == null ? 0 : hashCode_0(this.ab_1)) | 0;
  result = imul_0(result, 31) + (this.bb_1 == null ? 0 : hashCode_0(this.bb_1)) | 0;
  return result;
};
protoOf(CompletedContinuation_0).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof CompletedContinuation_0))
    return false;
  var tmp0_other_with_cast = other instanceof CompletedContinuation_0 ? other : THROW_CCE();
  if (!equals(this.xa_1, tmp0_other_with_cast.xa_1))
    return false;
  if (!equals(this.ya_1, tmp0_other_with_cast.ya_1))
    return false;
  if (!equals(this.za_1, tmp0_other_with_cast.za_1))
    return false;
  if (!equals(this.ab_1, tmp0_other_with_cast.ab_1))
    return false;
  if (!equals(this.bb_1, tmp0_other_with_cast.bb_1))
    return false;
  return true;
};
function CompletedExceptionally(cause, handled) {
  handled = handled === VOID ? false : handled;
  this.f8_1 = cause;
  this.g8_1 = atomic$boolean$1(handled);
}
protoOf(CompletedExceptionally).h8 = function () {
  return this.g8_1.kotlinx$atomicfu$value;
};
protoOf(CompletedExceptionally).sb = function () {
  return this.g8_1.atomicfu$compareAndSet(false, true);
};
protoOf(CompletedExceptionally).toString = function () {
  return get_classSimpleName(this) + '[' + this.f8_1.toString() + ']';
};
function CancelledContinuation(continuation, cause, handled) {
  CompletedExceptionally.call(this, cause == null ? CancellationException_init_$Create$_0('Continuation ' + toString_1(continuation) + ' was cancelled normally') : cause, handled);
  this.pa_1 = atomic$boolean$1(false);
}
protoOf(CancelledContinuation).qa = function () {
  return this.pa_1.atomicfu$compareAndSet(false, true);
};
function toState(_this__u8e3s4, caller) {
  // Inline function 'kotlin.fold' call
  var exception = Result__exceptionOrNull_impl_p6xea9(_this__u8e3s4);
  var tmp;
  if (exception == null) {
    var tmp_0 = _Result___get_value__impl__bjfvqg(_this__u8e3s4);
    tmp = (tmp_0 == null ? true : !(tmp_0 == null)) ? tmp_0 : THROW_CCE();
  } else {
    tmp = new CompletedExceptionally(recoverStackTrace(exception, caller));
  }
  return tmp;
}
function toState_0(_this__u8e3s4, onCancellation) {
  onCancellation = onCancellation === VOID ? null : onCancellation;
  // Inline function 'kotlin.fold' call
  var exception = Result__exceptionOrNull_impl_p6xea9(_this__u8e3s4);
  var tmp;
  if (exception == null) {
    var tmp_0 = _Result___get_value__impl__bjfvqg(_this__u8e3s4);
    var it = (tmp_0 == null ? true : !(tmp_0 == null)) ? tmp_0 : THROW_CCE();
    tmp = !(onCancellation == null) ? new CompletedWithCancellation(it, onCancellation) : it;
  } else {
    tmp = new CompletedExceptionally(exception);
  }
  return tmp;
}
function CompletedWithCancellation(result, onCancellation) {
  this.tb_1 = result;
  this.ub_1 = onCancellation;
}
protoOf(CompletedWithCancellation).toString = function () {
  return 'CompletedWithCancellation(result=' + toString_0(this.tb_1) + ', onCancellation=' + toString_1(this.ub_1) + ')';
};
protoOf(CompletedWithCancellation).hashCode = function () {
  var result = this.tb_1 == null ? 0 : hashCode_0(this.tb_1);
  result = imul_0(result, 31) + hashCode_0(this.ub_1) | 0;
  return result;
};
protoOf(CompletedWithCancellation).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof CompletedWithCancellation))
    return false;
  var tmp0_other_with_cast = other instanceof CompletedWithCancellation ? other : THROW_CCE();
  if (!equals(this.tb_1, tmp0_other_with_cast.tb_1))
    return false;
  if (!equals(this.ub_1, tmp0_other_with_cast.ub_1))
    return false;
  return true;
};
function CoroutineDispatcher$Key$_init_$lambda_akl8b5(it) {
  return it instanceof CoroutineDispatcher ? it : null;
}
function Key_0() {
  Key_instance_0 = this;
  var tmp = Key_instance;
  AbstractCoroutineContextKey.call(this, tmp, CoroutineDispatcher$Key$_init_$lambda_akl8b5);
}
var Key_instance_0;
function Key_getInstance_0() {
  if (Key_instance_0 == null)
    new Key_0();
  return Key_instance_0;
}
function CoroutineDispatcher() {
  Key_getInstance_0();
  AbstractCoroutineContextElement.call(this, Key_instance);
}
protoOf(CoroutineDispatcher).wb = function (context) {
  return true;
};
protoOf(CoroutineDispatcher).n4 = function (continuation) {
  return new DispatchedContinuation(this, continuation);
};
protoOf(CoroutineDispatcher).o4 = function (continuation) {
  var dispatched = continuation instanceof DispatchedContinuation ? continuation : THROW_CCE();
  dispatched.yb();
};
protoOf(CoroutineDispatcher).toString = function () {
  return get_classSimpleName(this) + '@' + get_hexAddress(this);
};
function handleCoroutineException(context, exception) {
  try {
    var tmp0_safe_receiver = context.m4(Key_instance_1);
    if (tmp0_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      tmp0_safe_receiver.zb(context, exception);
      return Unit_instance;
    }
  } catch ($p) {
    if ($p instanceof Error) {
      var t = $p;
      handleUncaughtCoroutineException(context, handlerException(exception, t));
      return Unit_instance;
    } else {
      throw $p;
    }
  }
  handleUncaughtCoroutineException(context, exception);
}
function Key_1() {
}
var Key_instance_1;
function Key_getInstance_1() {
  return Key_instance_1;
}
function handlerException(originalException, thrownException) {
  if (originalException === thrownException)
    return originalException;
  // Inline function 'kotlin.apply' call
  var this_0 = RuntimeException_init_$Create$_1('Exception while trying to handle coroutine exception', thrownException);
  addSuppressed(this_0, originalException);
  return this_0;
}
function CoroutineScope() {
}
function MainScope() {
  return new ContextScope(SupervisorJob().i7(Dispatchers_getInstance().ec()));
}
var CoroutineStart_DEFAULT_instance;
var CoroutineStart_LAZY_instance;
var CoroutineStart_ATOMIC_instance;
var CoroutineStart_UNDISPATCHED_instance;
var CoroutineStart_entriesInitialized;
function CoroutineStart_initEntries() {
  if (CoroutineStart_entriesInitialized)
    return Unit_instance;
  CoroutineStart_entriesInitialized = true;
  CoroutineStart_DEFAULT_instance = new CoroutineStart('DEFAULT', 0);
  CoroutineStart_LAZY_instance = new CoroutineStart('LAZY', 1);
  CoroutineStart_ATOMIC_instance = new CoroutineStart('ATOMIC', 2);
  CoroutineStart_UNDISPATCHED_instance = new CoroutineStart('UNDISPATCHED', 3);
}
function CoroutineStart(name, ordinal) {
  Enum.call(this, name, ordinal);
}
protoOf(CoroutineStart).q8 = function (block, receiver, completion) {
  var tmp;
  switch (this.o_1) {
    case 0:
      startCoroutineCancellable_0(block, receiver, completion);
      tmp = Unit_instance;
      break;
    case 2:
      startCoroutine(block, receiver, completion);
      tmp = Unit_instance;
      break;
    case 3:
      startCoroutineUndispatched(block, receiver, completion);
      tmp = Unit_instance;
      break;
    case 1:
      tmp = Unit_instance;
      break;
    default:
      noWhenBranchMatchedException();
      break;
  }
  return tmp;
};
protoOf(CoroutineStart).o9 = function () {
  return this === CoroutineStart_LAZY_getInstance();
};
function CoroutineStart_DEFAULT_getInstance() {
  CoroutineStart_initEntries();
  return CoroutineStart_DEFAULT_instance;
}
function CoroutineStart_LAZY_getInstance() {
  CoroutineStart_initEntries();
  return CoroutineStart_LAZY_instance;
}
function delta($this, unconfined) {
  return unconfined ? new Long(0, 1) : new Long(1, 0);
}
function EventLoop() {
  CoroutineDispatcher.call(this);
  this.gc_1 = new Long(0, 0);
  this.hc_1 = false;
  this.ic_1 = null;
}
protoOf(EventLoop).jc = function () {
  var tmp0_elvis_lhs = this.ic_1;
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return false;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var queue = tmp;
  var tmp1_elvis_lhs = queue.z6();
  var tmp_0;
  if (tmp1_elvis_lhs == null) {
    return false;
  } else {
    tmp_0 = tmp1_elvis_lhs;
  }
  var task = tmp_0;
  task.pb();
  return true;
};
protoOf(EventLoop).kc = function (task) {
  var tmp0_elvis_lhs = this.ic_1;
  var tmp;
  if (tmp0_elvis_lhs == null) {
    // Inline function 'kotlin.also' call
    var this_0 = ArrayDeque_init_$Create$();
    this.ic_1 = this_0;
    tmp = this_0;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var queue = tmp;
  queue.x6(task);
};
protoOf(EventLoop).lc = function () {
  return compare(this.gc_1, delta(this, true)) >= 0;
};
protoOf(EventLoop).mc = function () {
  var tmp0_safe_receiver = this.ic_1;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.g();
  return tmp1_elvis_lhs == null ? true : tmp1_elvis_lhs;
};
protoOf(EventLoop).nc = function (unconfined) {
  this.gc_1 = add(this.gc_1, delta(this, unconfined));
  if (!unconfined)
    this.hc_1 = true;
};
protoOf(EventLoop).oc = function (unconfined) {
  this.gc_1 = subtract(this.gc_1, delta(this, unconfined));
  if (compare(this.gc_1, new Long(0, 0)) > 0)
    return Unit_instance;
  // Inline function 'kotlinx.coroutines.assert' call
  if (this.hc_1) {
    this.pc();
  }
};
protoOf(EventLoop).pc = function () {
};
function ThreadLocalEventLoop() {
  ThreadLocalEventLoop_instance = this;
  this.qc_1 = commonThreadLocal(new Symbol_0('ThreadLocalEventLoop'));
}
protoOf(ThreadLocalEventLoop).rc = function () {
  var tmp0_elvis_lhs = this.qc_1.tc();
  var tmp;
  if (tmp0_elvis_lhs == null) {
    // Inline function 'kotlin.also' call
    var this_0 = createEventLoop();
    ThreadLocalEventLoop_getInstance().qc_1.uc(this_0);
    tmp = this_0;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
};
var ThreadLocalEventLoop_instance;
function ThreadLocalEventLoop_getInstance() {
  if (ThreadLocalEventLoop_instance == null)
    new ThreadLocalEventLoop();
  return ThreadLocalEventLoop_instance;
}
function CompletionHandlerException(message, cause) {
  RuntimeException_init_$Init$_1(message, cause, this);
  captureStack(this, CompletionHandlerException);
}
function CoroutinesInternalError(message, cause) {
  Error_init_$Init$_1(message, cause, this);
  captureStack(this, CoroutinesInternalError);
}
function Key_2() {
}
var Key_instance_2;
function Key_getInstance_2() {
  return Key_instance_2;
}
function Job() {
}
function ParentJob() {
}
function ChildHandle() {
}
function NonDisposableHandle() {
}
protoOf(NonDisposableHandle).ua = function () {
};
protoOf(NonDisposableHandle).f9 = function (cause) {
  return false;
};
protoOf(NonDisposableHandle).toString = function () {
  return 'NonDisposableHandle';
};
var NonDisposableHandle_instance;
function NonDisposableHandle_getInstance() {
  return NonDisposableHandle_instance;
}
function get_COMPLETING_ALREADY() {
  _init_properties_JobSupport_kt__68f172();
  return COMPLETING_ALREADY;
}
var COMPLETING_ALREADY;
function get_COMPLETING_WAITING_CHILDREN() {
  _init_properties_JobSupport_kt__68f172();
  return COMPLETING_WAITING_CHILDREN;
}
var COMPLETING_WAITING_CHILDREN;
function get_COMPLETING_RETRY() {
  _init_properties_JobSupport_kt__68f172();
  return COMPLETING_RETRY;
}
var COMPLETING_RETRY;
function get_TOO_LATE_TO_CANCEL() {
  _init_properties_JobSupport_kt__68f172();
  return TOO_LATE_TO_CANCEL;
}
var TOO_LATE_TO_CANCEL;
function get_SEALED() {
  _init_properties_JobSupport_kt__68f172();
  return SEALED;
}
var SEALED;
function get_EMPTY_NEW() {
  _init_properties_JobSupport_kt__68f172();
  return EMPTY_NEW;
}
var EMPTY_NEW;
function get_EMPTY_ACTIVE() {
  _init_properties_JobSupport_kt__68f172();
  return EMPTY_ACTIVE;
}
var EMPTY_ACTIVE;
function Empty(isActive) {
  this.vc_1 = isActive;
}
protoOf(Empty).a8 = function () {
  return this.vc_1;
};
protoOf(Empty).wc = function () {
  return null;
};
protoOf(Empty).toString = function () {
  return 'Empty{' + (this.vc_1 ? 'Active' : 'New') + '}';
};
function Incomplete() {
}
function NodeList() {
  LinkedListHead.call(this);
}
protoOf(NodeList).a8 = function () {
  return true;
};
protoOf(NodeList).wc = function () {
  return this;
};
protoOf(NodeList).ad = function (state) {
  // Inline function 'kotlin.text.buildString' call
  // Inline function 'kotlin.apply' call
  var this_0 = StringBuilder_init_$Create$_0();
  this_0.l3('List{');
  this_0.l3(state);
  this_0.l3('}[');
  var first = true;
  // Inline function 'kotlinx.coroutines.internal.LinkedListHead.forEach' call
  var cur = this.bd_1;
  while (!equals(cur, this)) {
    if (cur instanceof JobNode) {
      var node = cur;
      if (first)
        first = false;
      else {
        this_0.l3(', ');
      }
      this_0.k3(node);
    }
    cur = cur.bd_1;
  }
  this_0.l3(']');
  return this_0.toString();
};
protoOf(NodeList).toString = function () {
  return get_DEBUG() ? this.ad('Active') : protoOf(LinkedListHead).toString.call(this);
};
function JobNode() {
  CompletionHandlerBase.call(this);
}
protoOf(JobNode).od = function () {
  var tmp = this.nd_1;
  if (!(tmp == null))
    return tmp;
  else {
    throwUninitializedPropertyAccessException('job');
  }
};
protoOf(JobNode).a8 = function () {
  return true;
};
protoOf(JobNode).wc = function () {
  return null;
};
protoOf(JobNode).ua = function () {
  return this.od().c9(this);
};
protoOf(JobNode).toString = function () {
  return get_classSimpleName(this) + '@' + get_hexAddress(this) + '[job@' + get_hexAddress(this.od()) + ']';
};
function _set_exceptionsHolder__tqm22h($this, value) {
  $this.td_1.kotlinx$atomicfu$value = value;
}
function _get_exceptionsHolder__nhszp($this) {
  return $this.td_1.kotlinx$atomicfu$value;
}
function allocateList($this) {
  return ArrayList_init_$Create$_0(4);
}
function finalizeFinishingState($this, state, proposedUpdate) {
  // Inline function 'kotlinx.coroutines.assert' call
  // Inline function 'kotlinx.coroutines.assert' call
  // Inline function 'kotlinx.coroutines.assert' call
  var tmp0_safe_receiver = proposedUpdate instanceof CompletedExceptionally ? proposedUpdate : null;
  var proposedException = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.f8_1;
  var wasCancelling;
  // Inline function 'kotlinx.coroutines.internal.synchronized' call
  // Inline function 'kotlin.contracts.contract' call
  // Inline function 'kotlinx.coroutines.internal.synchronizedImpl' call
  wasCancelling = state.ud();
  var exceptions = state.vd(proposedException);
  var finalCause = getFinalRootCause($this, state, exceptions);
  if (!(finalCause == null)) {
    addSuppressedExceptions($this, finalCause, exceptions);
  }
  var finalException = finalCause;
  var finalState = finalException == null ? proposedUpdate : finalException === proposedException ? proposedUpdate : new CompletedExceptionally(finalException);
  if (!(finalException == null)) {
    var handled = cancelParent($this, finalException) || $this.m9(finalException);
    if (handled) {
      (finalState instanceof CompletedExceptionally ? finalState : THROW_CCE()).sb();
    }
  }
  if (!wasCancelling) {
    $this.j9(finalException);
  }
  $this.e8(finalState);
  var casSuccess = $this.t7_1.atomicfu$compareAndSet(state, boxIncomplete(finalState));
  // Inline function 'kotlinx.coroutines.assert' call
  completeStateFinalization($this, state, finalState);
  return finalState;
}
function getFinalRootCause($this, state, exceptions) {
  if (exceptions.g()) {
    if (state.ud()) {
      // Inline function 'kotlinx.coroutines.JobSupport.defaultCancellationException' call
      return new JobCancellationException(null == null ? $this.d8() : null, null, $this);
    }
    return null;
  }
  var tmp$ret$2;
  $l$block: {
    // Inline function 'kotlin.collections.firstOrNull' call
    var _iterator__ex2g4s = exceptions.c();
    while (_iterator__ex2g4s.d()) {
      var element = _iterator__ex2g4s.e();
      if (!(element instanceof CancellationException)) {
        tmp$ret$2 = element;
        break $l$block;
      }
    }
    tmp$ret$2 = null;
  }
  var firstNonCancellation = tmp$ret$2;
  if (!(firstNonCancellation == null))
    return firstNonCancellation;
  var first = exceptions.i(0);
  if (first instanceof TimeoutCancellationException) {
    var tmp$ret$4;
    $l$block_0: {
      // Inline function 'kotlin.collections.firstOrNull' call
      var _iterator__ex2g4s_0 = exceptions.c();
      while (_iterator__ex2g4s_0.d()) {
        var element_0 = _iterator__ex2g4s_0.e();
        var tmp;
        if (!(element_0 === first)) {
          tmp = element_0 instanceof TimeoutCancellationException;
        } else {
          tmp = false;
        }
        if (tmp) {
          tmp$ret$4 = element_0;
          break $l$block_0;
        }
      }
      tmp$ret$4 = null;
    }
    var detailedTimeoutException = tmp$ret$4;
    if (!(detailedTimeoutException == null))
      return detailedTimeoutException;
  }
  return first;
}
function addSuppressedExceptions($this, rootCause, exceptions) {
  if (exceptions.f() <= 1)
    return Unit_instance;
  var seenExceptions = identitySet(exceptions.f());
  var unwrappedCause = unwrap(rootCause);
  var tmp0_iterator = exceptions.c();
  while (tmp0_iterator.d()) {
    var exception = tmp0_iterator.e();
    var unwrapped = unwrap(exception);
    var tmp;
    var tmp_0;
    if (!(unwrapped === rootCause) && !(unwrapped === unwrappedCause)) {
      tmp_0 = !(unwrapped instanceof CancellationException);
    } else {
      tmp_0 = false;
    }
    if (tmp_0) {
      tmp = seenExceptions.b1(unwrapped);
    } else {
      tmp = false;
    }
    if (tmp) {
      addSuppressed(rootCause, unwrapped);
    }
  }
}
function tryFinalizeSimpleState($this, state, update) {
  // Inline function 'kotlinx.coroutines.assert' call
  // Inline function 'kotlinx.coroutines.assert' call
  if (!$this.t7_1.atomicfu$compareAndSet(state, boxIncomplete(update)))
    return false;
  $this.j9(null);
  $this.e8(update);
  completeStateFinalization($this, state, update);
  return true;
}
function completeStateFinalization($this, state, update) {
  var tmp0_safe_receiver = $this.s8();
  if (tmp0_safe_receiver == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    tmp0_safe_receiver.ua();
    $this.r8(NonDisposableHandle_instance);
  }
  var tmp1_safe_receiver = update instanceof CompletedExceptionally ? update : null;
  var cause = tmp1_safe_receiver == null ? null : tmp1_safe_receiver.f8_1;
  if (state instanceof JobNode) {
    try {
      state.invoke(cause);
    } catch ($p) {
      if ($p instanceof Error) {
        var ex = $p;
        $this.l8(new CompletionHandlerException('Exception in completion handler ' + toString_1(state) + ' for ' + $this.toString(), ex));
      } else {
        throw $p;
      }
    }
  } else {
    var tmp2_safe_receiver = state.wc();
    if (tmp2_safe_receiver == null)
      null;
    else {
      notifyCompletion($this, tmp2_safe_receiver, cause);
    }
  }
}
function notifyCancelling($this, list, cause) {
  $this.j9(cause);
  // Inline function 'kotlinx.coroutines.JobSupport.notifyHandlers' call
  var exception = null;
  // Inline function 'kotlinx.coroutines.internal.LinkedListHead.forEach' call
  var cur = list.bd_1;
  while (!equals(cur, list)) {
    if (cur instanceof JobCancellingNode) {
      var node = cur;
      try {
        node.invoke(cause);
      } catch ($p) {
        if ($p instanceof Error) {
          var ex = $p;
          var tmp0_safe_receiver = exception;
          var tmp;
          if (tmp0_safe_receiver == null) {
            tmp = null;
          } else {
            // Inline function 'kotlin.apply' call
            addSuppressed(tmp0_safe_receiver, ex);
            tmp = tmp0_safe_receiver;
          }
          if (tmp == null) {
            // Inline function 'kotlin.run' call
            exception = new CompletionHandlerException('Exception in completion handler ' + node.toString() + ' for ' + $this.toString(), ex);
          }
        } else {
          throw $p;
        }
      }
    }
    cur = cur.bd_1;
  }
  var tmp0_safe_receiver_0 = exception;
  if (tmp0_safe_receiver_0 == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    $this.l8(tmp0_safe_receiver_0);
  }
  cancelParent($this, cause);
}
function cancelParent($this, cause) {
  if ($this.k9())
    return true;
  var isCancellation = cause instanceof CancellationException;
  var parent = $this.s8();
  if (parent === null || parent === NonDisposableHandle_instance) {
    return isCancellation;
  }
  return parent.f9(cause) || isCancellation;
}
function notifyCompletion($this, _this__u8e3s4, cause) {
  // Inline function 'kotlinx.coroutines.JobSupport.notifyHandlers' call
  var exception = null;
  // Inline function 'kotlinx.coroutines.internal.LinkedListHead.forEach' call
  var cur = _this__u8e3s4.bd_1;
  while (!equals(cur, _this__u8e3s4)) {
    if (cur instanceof JobNode) {
      var node = cur;
      try {
        node.invoke(cause);
      } catch ($p) {
        if ($p instanceof Error) {
          var ex = $p;
          var tmp0_safe_receiver = exception;
          var tmp;
          if (tmp0_safe_receiver == null) {
            tmp = null;
          } else {
            // Inline function 'kotlin.apply' call
            addSuppressed(tmp0_safe_receiver, ex);
            tmp = tmp0_safe_receiver;
          }
          if (tmp == null) {
            // Inline function 'kotlin.run' call
            exception = new CompletionHandlerException('Exception in completion handler ' + node.toString() + ' for ' + $this.toString(), ex);
          }
        } else {
          throw $p;
        }
      }
    }
    cur = cur.bd_1;
  }
  var tmp0_safe_receiver_0 = exception;
  if (tmp0_safe_receiver_0 == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    $this.l8(tmp0_safe_receiver_0);
  }
  return Unit_instance;
}
function startInternal($this, state) {
  if (state instanceof Empty) {
    if (state.vc_1)
      return 0;
    if (!$this.t7_1.atomicfu$compareAndSet(state, get_EMPTY_ACTIVE()))
      return -1;
    $this.w8();
    return 1;
  } else {
    if (state instanceof InactiveNodeList) {
      if (!$this.t7_1.atomicfu$compareAndSet(state, state.wd_1))
        return -1;
      $this.w8();
      return 1;
    } else {
      return 0;
    }
  }
}
function makeNode($this, handler, onCancelling) {
  var tmp;
  if (onCancelling) {
    var tmp0_elvis_lhs = handler instanceof JobCancellingNode ? handler : null;
    tmp = tmp0_elvis_lhs == null ? new InvokeOnCancelling(handler) : tmp0_elvis_lhs;
  } else {
    var tmp1_safe_receiver = handler instanceof JobNode ? handler : null;
    var tmp_0;
    if (tmp1_safe_receiver == null) {
      tmp_0 = null;
    } else {
      // Inline function 'kotlin.also' call
      // Inline function 'kotlinx.coroutines.assert' call
      tmp_0 = tmp1_safe_receiver;
    }
    var tmp2_elvis_lhs = tmp_0;
    tmp = tmp2_elvis_lhs == null ? new InvokeOnCompletion(handler) : tmp2_elvis_lhs;
  }
  var node = tmp;
  node.nd_1 = $this;
  return node;
}
function addLastAtomic($this, expect, list, node) {
  var tmp$ret$1;
  $l$block: {
    // Inline function 'kotlinx.coroutines.internal.LinkedListNode.addLastIf' call
    if (!($this.t8() === expect)) {
      tmp$ret$1 = false;
      break $l$block;
    }
    list.id(node);
    tmp$ret$1 = true;
  }
  return tmp$ret$1;
}
function promoteEmptyToNodeList($this, state) {
  var list = new NodeList();
  var update = state.vc_1 ? list : new InactiveNodeList(list);
  $this.t7_1.atomicfu$compareAndSet(state, update);
}
function promoteSingleToNodeList($this, state) {
  state.pd(new NodeList());
  // Inline function 'kotlinx.coroutines.internal.LinkedListNode.nextNode' call
  var list = state.bd_1;
  $this.t7_1.atomicfu$compareAndSet(state, list);
}
function cancelMakeCompleting($this, cause) {
  // Inline function 'kotlinx.coroutines.JobSupport.loopOnState' call
  while (true) {
    var state = $this.t8();
    var tmp;
    if (!(!(state == null) ? isInterface(state, Incomplete) : false)) {
      tmp = true;
    } else {
      var tmp_0;
      if (state instanceof Finishing) {
        tmp_0 = state.xd();
      } else {
        tmp_0 = false;
      }
      tmp = tmp_0;
    }
    if (tmp) {
      return get_COMPLETING_ALREADY();
    }
    var proposedUpdate = new CompletedExceptionally(createCauseException($this, cause));
    var finalState = tryMakeCompleting($this, state, proposedUpdate);
    if (!(finalState === get_COMPLETING_RETRY()))
      return finalState;
  }
}
function createCauseException($this, cause) {
  var tmp;
  if (cause == null ? true : cause instanceof Error) {
    var tmp_0;
    if (cause == null) {
      // Inline function 'kotlinx.coroutines.JobSupport.defaultCancellationException' call
      tmp_0 = new JobCancellationException(null == null ? $this.d8() : null, null, $this);
    } else {
      tmp_0 = cause;
    }
    tmp = tmp_0;
  } else {
    tmp = ((!(cause == null) ? isInterface(cause, ParentJob) : false) ? cause : THROW_CCE()).h9();
  }
  return tmp;
}
function makeCancelling($this, cause) {
  var causeExceptionCache = null;
  // Inline function 'kotlinx.coroutines.JobSupport.loopOnState' call
  while (true) {
    var tmp0 = $this.t8();
    $l$block: {
      if (tmp0 instanceof Finishing) {
        // Inline function 'kotlinx.coroutines.internal.synchronized' call
        // Inline function 'kotlin.contracts.contract' call
        // Inline function 'kotlinx.coroutines.internal.synchronizedImpl' call
        if (tmp0.yd())
          return get_TOO_LATE_TO_CANCEL();
        var wasCancelling = tmp0.ud();
        if (!(cause == null) || !wasCancelling) {
          var tmp0_elvis_lhs = causeExceptionCache;
          var tmp;
          if (tmp0_elvis_lhs == null) {
            // Inline function 'kotlin.also' call
            var this_0 = createCauseException($this, cause);
            causeExceptionCache = this_0;
            tmp = this_0;
          } else {
            tmp = tmp0_elvis_lhs;
          }
          var causeException = tmp;
          tmp0.zd(causeException);
        }
        // Inline function 'kotlin.takeIf' call
        var this_1 = tmp0.ae();
        var tmp_0;
        if (!wasCancelling) {
          tmp_0 = this_1;
        } else {
          tmp_0 = null;
        }
        var notifyRootCause = tmp_0;
        if (notifyRootCause == null)
          null;
        else {
          // Inline function 'kotlin.let' call
          notifyCancelling($this, tmp0.qd_1, notifyRootCause);
        }
        return get_COMPLETING_ALREADY();
      } else {
        if (!(tmp0 == null) ? isInterface(tmp0, Incomplete) : false) {
          var tmp2_elvis_lhs = causeExceptionCache;
          var tmp_1;
          if (tmp2_elvis_lhs == null) {
            // Inline function 'kotlin.also' call
            var this_2 = createCauseException($this, cause);
            causeExceptionCache = this_2;
            tmp_1 = this_2;
          } else {
            tmp_1 = tmp2_elvis_lhs;
          }
          var causeException_0 = tmp_1;
          if (tmp0.a8()) {
            if (tryMakeCancelling($this, tmp0, causeException_0))
              return get_COMPLETING_ALREADY();
          } else {
            var finalState = tryMakeCompleting($this, tmp0, new CompletedExceptionally(causeException_0));
            if (finalState === get_COMPLETING_ALREADY()) {
              // Inline function 'kotlin.error' call
              var message = 'Cannot happen in ' + toString_0(tmp0);
              throw IllegalStateException_init_$Create$_0(toString_1(message));
            } else if (finalState === get_COMPLETING_RETRY()) {
              break $l$block;
            } else
              return finalState;
          }
        } else {
          return get_TOO_LATE_TO_CANCEL();
        }
      }
    }
  }
}
function getOrPromoteCancellingList($this, state) {
  var tmp1_elvis_lhs = state.wc();
  var tmp;
  if (tmp1_elvis_lhs == null) {
    var tmp_0;
    if (state instanceof Empty) {
      tmp_0 = new NodeList();
    } else {
      if (state instanceof JobNode) {
        promoteSingleToNodeList($this, state);
        tmp_0 = null;
      } else {
        var message = 'State should have list: ' + toString_1(state);
        throw IllegalStateException_init_$Create$_0(toString_1(message));
      }
    }
    tmp = tmp_0;
  } else {
    tmp = tmp1_elvis_lhs;
  }
  return tmp;
}
function tryMakeCancelling($this, state, rootCause) {
  // Inline function 'kotlinx.coroutines.assert' call
  // Inline function 'kotlinx.coroutines.assert' call
  var tmp0_elvis_lhs = getOrPromoteCancellingList($this, state);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return false;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var list = tmp;
  var cancelling = new Finishing(list, false, rootCause);
  if (!$this.t7_1.atomicfu$compareAndSet(state, cancelling))
    return false;
  notifyCancelling($this, list, rootCause);
  return true;
}
function tryMakeCompleting($this, state, proposedUpdate) {
  if (!(!(state == null) ? isInterface(state, Incomplete) : false))
    return get_COMPLETING_ALREADY();
  var tmp;
  var tmp_0;
  var tmp_1;
  if (state instanceof Empty) {
    tmp_1 = true;
  } else {
    tmp_1 = state instanceof JobNode;
  }
  if (tmp_1) {
    tmp_0 = !(state instanceof ChildHandleNode);
  } else {
    tmp_0 = false;
  }
  if (tmp_0) {
    tmp = !(proposedUpdate instanceof CompletedExceptionally);
  } else {
    tmp = false;
  }
  if (tmp) {
    if (tryFinalizeSimpleState($this, state, proposedUpdate)) {
      return proposedUpdate;
    }
    return get_COMPLETING_RETRY();
  }
  return tryMakeCompletingSlowPath($this, state, proposedUpdate);
}
function tryMakeCompletingSlowPath($this, state, proposedUpdate) {
  var tmp0_elvis_lhs = getOrPromoteCancellingList($this, state);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return get_COMPLETING_RETRY();
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var list = tmp;
  var tmp1_elvis_lhs = state instanceof Finishing ? state : null;
  var finishing = tmp1_elvis_lhs == null ? new Finishing(list, false, null) : tmp1_elvis_lhs;
  var notifyRootCause = null;
  // Inline function 'kotlinx.coroutines.internal.synchronized' call
  // Inline function 'kotlin.contracts.contract' call
  // Inline function 'kotlinx.coroutines.internal.synchronizedImpl' call
  if (finishing.xd())
    return get_COMPLETING_ALREADY();
  finishing.be(true);
  if (!(finishing === state)) {
    if (!$this.t7_1.atomicfu$compareAndSet(state, finishing))
      return get_COMPLETING_RETRY();
  }
  // Inline function 'kotlinx.coroutines.assert' call
  var wasCancelling = finishing.ud();
  var tmp0_safe_receiver = proposedUpdate instanceof CompletedExceptionally ? proposedUpdate : null;
  if (tmp0_safe_receiver == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    finishing.zd(tmp0_safe_receiver.f8_1);
  }
  // Inline function 'kotlin.takeIf' call
  var this_0 = finishing.ae();
  var tmp_0;
  if (!wasCancelling) {
    tmp_0 = this_0;
  } else {
    tmp_0 = null;
  }
  notifyRootCause = tmp_0;
  var tmp2_safe_receiver = notifyRootCause;
  if (tmp2_safe_receiver == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    notifyCancelling($this, list, tmp2_safe_receiver);
  }
  var child = firstChild($this, state);
  if (!(child == null) && tryWaitForChild($this, finishing, child, proposedUpdate))
    return get_COMPLETING_WAITING_CHILDREN();
  return finalizeFinishingState($this, finishing, proposedUpdate);
}
function _get_exceptionOrNull__b3j7js($this, _this__u8e3s4) {
  var tmp0_safe_receiver = _this__u8e3s4 instanceof CompletedExceptionally ? _this__u8e3s4 : null;
  return tmp0_safe_receiver == null ? null : tmp0_safe_receiver.f8_1;
}
function firstChild($this, state) {
  var tmp1_elvis_lhs = state instanceof ChildHandleNode ? state : null;
  var tmp;
  if (tmp1_elvis_lhs == null) {
    var tmp0_safe_receiver = state.wc();
    tmp = tmp0_safe_receiver == null ? null : nextChild($this, tmp0_safe_receiver);
  } else {
    tmp = tmp1_elvis_lhs;
  }
  return tmp;
}
function tryWaitForChild($this, state, child, proposedUpdate) {
  var $this_0 = $this;
  var state_0 = state;
  var child_0 = child;
  var proposedUpdate_0 = proposedUpdate;
  $l$1: do {
    $l$0: do {
      var tmp = child_0.ge_1;
      // Inline function 'kotlinx.coroutines.asHandler' call
      // Inline function 'kotlin.js.asDynamic' call
      var tmp$ret$1 = new ChildCompletion($this_0, state_0, child_0, proposedUpdate_0);
      var handle = tmp.b9(VOID, false, tmp$ret$1);
      if (!(handle === NonDisposableHandle_instance))
        return true;
      var tmp0_elvis_lhs = nextChild($this_0, child_0);
      var tmp_0;
      if (tmp0_elvis_lhs == null) {
        return false;
      } else {
        tmp_0 = tmp0_elvis_lhs;
      }
      var nextChild_0 = tmp_0;
      var tmp0 = $this_0;
      var tmp1 = state_0;
      var tmp3 = proposedUpdate_0;
      $this_0 = tmp0;
      state_0 = tmp1;
      child_0 = nextChild_0;
      proposedUpdate_0 = tmp3;
      continue $l$0;
    }
     while (false);
  }
   while (true);
}
function continueCompleting($this, state, lastChild, proposedUpdate) {
  // Inline function 'kotlinx.coroutines.assert' call
  var waitChild = nextChild($this, lastChild);
  if (!(waitChild == null) && tryWaitForChild($this, state, waitChild, proposedUpdate))
    return Unit_instance;
  var finalState = finalizeFinishingState($this, state, proposedUpdate);
  $this.k8(finalState);
}
function nextChild($this, _this__u8e3s4) {
  var cur = _this__u8e3s4;
  $l$loop: while (true) {
    // Inline function 'kotlinx.coroutines.internal.LinkedListNode.isRemoved' call
    if (!cur.dd_1) {
      break $l$loop;
    }
    // Inline function 'kotlinx.coroutines.internal.LinkedListNode.prevNode' call
    cur = cur.cd_1;
  }
  $l$loop_0: while (true) {
    // Inline function 'kotlinx.coroutines.internal.LinkedListNode.nextNode' call
    cur = cur.bd_1;
    // Inline function 'kotlinx.coroutines.internal.LinkedListNode.isRemoved' call
    if (cur.dd_1)
      continue $l$loop_0;
    if (cur instanceof ChildHandleNode)
      return cur;
    if (cur instanceof NodeList)
      return null;
  }
}
function stateString($this, state) {
  var tmp;
  if (state instanceof Finishing) {
    tmp = state.ud() ? 'Cancelling' : state.xd() ? 'Completing' : 'Active';
  } else {
    if (!(state == null) ? isInterface(state, Incomplete) : false) {
      tmp = state.a8() ? 'Active' : 'New';
    } else {
      if (state instanceof CompletedExceptionally) {
        tmp = 'Cancelled';
      } else {
        tmp = 'Completed';
      }
    }
  }
  return tmp;
}
function Finishing(list, isCompleting, rootCause) {
  SynchronizedObject.call(this);
  this.qd_1 = list;
  this.rd_1 = atomic$boolean$1(isCompleting);
  this.sd_1 = atomic$ref$1(rootCause);
  this.td_1 = atomic$ref$1(null);
}
protoOf(Finishing).wc = function () {
  return this.qd_1;
};
protoOf(Finishing).be = function (value) {
  this.rd_1.kotlinx$atomicfu$value = value;
};
protoOf(Finishing).xd = function () {
  return this.rd_1.kotlinx$atomicfu$value;
};
protoOf(Finishing).he = function (value) {
  this.sd_1.kotlinx$atomicfu$value = value;
};
protoOf(Finishing).ae = function () {
  return this.sd_1.kotlinx$atomicfu$value;
};
protoOf(Finishing).yd = function () {
  return _get_exceptionsHolder__nhszp(this) === get_SEALED();
};
protoOf(Finishing).ud = function () {
  return !(this.ae() == null);
};
protoOf(Finishing).a8 = function () {
  return this.ae() == null;
};
protoOf(Finishing).vd = function (proposedException) {
  var eh = _get_exceptionsHolder__nhszp(this);
  var tmp;
  if (eh == null) {
    tmp = allocateList(this);
  } else {
    if (eh instanceof Error) {
      // Inline function 'kotlin.also' call
      var this_0 = allocateList(this);
      this_0.b1(eh);
      tmp = this_0;
    } else {
      if (eh instanceof ArrayList) {
        tmp = eh instanceof ArrayList ? eh : THROW_CCE();
      } else {
        var message = 'State is ' + toString_0(eh);
        throw IllegalStateException_init_$Create$_0(toString_1(message));
      }
    }
  }
  var list = tmp;
  var rootCause = this.ae();
  if (rootCause == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    list.h1(0, rootCause);
  }
  if (!(proposedException == null) && !equals(proposedException, rootCause)) {
    list.b1(proposedException);
  }
  _set_exceptionsHolder__tqm22h(this, get_SEALED());
  return list;
};
protoOf(Finishing).zd = function (exception) {
  var rootCause = this.ae();
  if (rootCause == null) {
    this.he(exception);
    return Unit_instance;
  }
  if (exception === rootCause)
    return Unit_instance;
  var eh = _get_exceptionsHolder__nhszp(this);
  if (eh == null) {
    _set_exceptionsHolder__tqm22h(this, exception);
  } else {
    if (eh instanceof Error) {
      if (exception === eh)
        return Unit_instance;
      // Inline function 'kotlin.apply' call
      var this_0 = allocateList(this);
      this_0.b1(eh);
      this_0.b1(exception);
      _set_exceptionsHolder__tqm22h(this, this_0);
    } else {
      if (eh instanceof ArrayList) {
        (eh instanceof ArrayList ? eh : THROW_CCE()).b1(exception);
      } else {
        // Inline function 'kotlin.error' call
        var message = 'State is ' + toString_0(eh);
        throw IllegalStateException_init_$Create$_0(toString_1(message));
      }
    }
  }
};
protoOf(Finishing).toString = function () {
  return 'Finishing[cancelling=' + this.ud() + ', completing=' + this.xd() + ', rootCause=' + toString_0(this.ae()) + ', exceptions=' + toString_0(_get_exceptionsHolder__nhszp(this)) + ', list=' + this.qd_1.toString() + ']';
};
function ChildCompletion(parent, state, child, proposedUpdate) {
  JobNode.call(this);
  this.me_1 = parent;
  this.ne_1 = state;
  this.oe_1 = child;
  this.pe_1 = proposedUpdate;
}
protoOf(ChildCompletion).qe = function (cause) {
  continueCompleting(this.me_1, this.ne_1, this.oe_1, this.pe_1);
};
protoOf(ChildCompletion).invoke = function (cause) {
  return this.qe(cause);
};
function JobSupport(active) {
  this.t7_1 = atomic$ref$1(active ? get_EMPTY_ACTIVE() : get_EMPTY_NEW());
  this.u7_1 = atomic$ref$1(null);
}
protoOf(JobSupport).k = function () {
  return Key_instance_2;
};
protoOf(JobSupport).r8 = function (value) {
  this.u7_1.kotlinx$atomicfu$value = value;
};
protoOf(JobSupport).s8 = function () {
  return this.u7_1.kotlinx$atomicfu$value;
};
protoOf(JobSupport).v7 = function (parent) {
  // Inline function 'kotlinx.coroutines.assert' call
  if (parent == null) {
    this.r8(NonDisposableHandle_instance);
    return Unit_instance;
  }
  parent.v8();
  var handle = parent.i9(this);
  this.r8(handle);
  if (this.u8()) {
    handle.ua();
    this.r8(NonDisposableHandle_instance);
  }
};
protoOf(JobSupport).t8 = function () {
  // Inline function 'kotlinx.atomicfu.loop' call
  var this_0 = this.t7_1;
  while (true) {
    var state = this_0.kotlinx$atomicfu$value;
    if (!(state instanceof OpDescriptor))
      return state;
    state.re(this);
  }
};
protoOf(JobSupport).a8 = function () {
  var state = this.t8();
  var tmp;
  if (!(state == null) ? isInterface(state, Incomplete) : false) {
    tmp = state.a8();
  } else {
    tmp = false;
  }
  return tmp;
};
protoOf(JobSupport).u8 = function () {
  var tmp = this.t8();
  return !(!(tmp == null) ? isInterface(tmp, Incomplete) : false);
};
protoOf(JobSupport).v8 = function () {
  // Inline function 'kotlinx.coroutines.JobSupport.loopOnState' call
  while (true) {
    var state = this.t8();
    var tmp0_subject = startInternal(this, state);
    if (tmp0_subject === 0)
      return false;
    else if (tmp0_subject === 1)
      return true;
  }
};
protoOf(JobSupport).w8 = function () {
};
protoOf(JobSupport).x8 = function () {
  var state = this.t8();
  var tmp;
  if (state instanceof Finishing) {
    var tmp0_safe_receiver = state.ae();
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : this.y8(tmp0_safe_receiver, get_classSimpleName(this) + ' is cancelling');
    var tmp_0;
    if (tmp1_elvis_lhs == null) {
      var message = 'Job is still new or active: ' + this.toString();
      throw IllegalStateException_init_$Create$_0(toString_1(message));
    } else {
      tmp_0 = tmp1_elvis_lhs;
    }
    tmp = tmp_0;
  } else {
    if (!(state == null) ? isInterface(state, Incomplete) : false) {
      var message_0 = 'Job is still new or active: ' + this.toString();
      throw IllegalStateException_init_$Create$_0(toString_1(message_0));
    } else {
      if (state instanceof CompletedExceptionally) {
        tmp = this.z8(state.f8_1);
      } else {
        tmp = new JobCancellationException(get_classSimpleName(this) + ' has completed normally', null, this);
      }
    }
  }
  return tmp;
};
protoOf(JobSupport).y8 = function (_this__u8e3s4, message) {
  var tmp0_elvis_lhs = _this__u8e3s4 instanceof CancellationException ? _this__u8e3s4 : null;
  var tmp;
  if (tmp0_elvis_lhs == null) {
    // Inline function 'kotlinx.coroutines.JobSupport.defaultCancellationException' call
    tmp = new JobCancellationException(message == null ? this.d8() : message, _this__u8e3s4, this);
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
};
protoOf(JobSupport).z8 = function (_this__u8e3s4, message, $super) {
  message = message === VOID ? null : message;
  return $super === VOID ? this.y8(_this__u8e3s4, message) : $super.y8.call(this, _this__u8e3s4, message);
};
protoOf(JobSupport).a9 = function (onCancelling, invokeImmediately, handler) {
  var node = makeNode(this, handler, onCancelling);
  // Inline function 'kotlinx.coroutines.JobSupport.loopOnState' call
  while (true) {
    var tmp0 = this.t8();
    $l$block: {
      if (tmp0 instanceof Empty) {
        if (tmp0.vc_1) {
          if (this.t7_1.atomicfu$compareAndSet(tmp0, node))
            return node;
        } else {
          promoteEmptyToNodeList(this, tmp0);
        }
      } else {
        if (!(tmp0 == null) ? isInterface(tmp0, Incomplete) : false) {
          var list = tmp0.wc();
          if (list == null) {
            promoteSingleToNodeList(this, tmp0 instanceof JobNode ? tmp0 : THROW_CCE());
          } else {
            var rootCause = null;
            var handle = NonDisposableHandle_instance;
            var tmp;
            if (onCancelling) {
              tmp = tmp0 instanceof Finishing;
            } else {
              tmp = false;
            }
            if (tmp) {
              // Inline function 'kotlinx.coroutines.internal.synchronized' call
              // Inline function 'kotlin.contracts.contract' call
              // Inline function 'kotlinx.coroutines.internal.synchronizedImpl' call
              rootCause = tmp0.ae();
              var tmp_0;
              if (rootCause == null) {
                tmp_0 = true;
              } else {
                var tmp_1;
                // Inline function 'kotlinx.coroutines.isHandlerOf' call
                if (handler instanceof ChildHandleNode) {
                  tmp_1 = !tmp0.xd();
                } else {
                  tmp_1 = false;
                }
                tmp_0 = tmp_1;
              }
              if (tmp_0) {
                if (!addLastAtomic(this, tmp0, list, node)) {
                  break $l$block;
                }
                if (rootCause == null)
                  return node;
                handle = node;
              }
            }
            if (!(rootCause == null)) {
              if (invokeImmediately) {
                invokeIt(handler, rootCause);
              }
              return handle;
            } else {
              if (addLastAtomic(this, tmp0, list, node))
                return node;
            }
          }
        } else {
          if (invokeImmediately) {
            var tmp1_safe_receiver = tmp0 instanceof CompletedExceptionally ? tmp0 : null;
            invokeIt(handler, tmp1_safe_receiver == null ? null : tmp1_safe_receiver.f8_1);
          }
          return NonDisposableHandle_instance;
        }
      }
    }
  }
};
protoOf(JobSupport).c9 = function (node) {
  // Inline function 'kotlinx.coroutines.JobSupport.loopOnState' call
  while (true) {
    var state = this.t8();
    if (state instanceof JobNode) {
      if (!(state === node))
        return Unit_instance;
      if (this.t7_1.atomicfu$compareAndSet(state, get_EMPTY_ACTIVE()))
        return Unit_instance;
    } else {
      if (!(state == null) ? isInterface(state, Incomplete) : false) {
        if (!(state.wc() == null)) {
          node.hd();
        }
        return Unit_instance;
      } else {
        return Unit_instance;
      }
    }
  }
};
protoOf(JobSupport).d9 = function () {
  return false;
};
protoOf(JobSupport).d8 = function () {
  return 'Job was cancelled';
};
protoOf(JobSupport).e9 = function (parentJob) {
  this.g9(parentJob);
};
protoOf(JobSupport).f9 = function (cause) {
  if (cause instanceof CancellationException)
    return true;
  return this.g9(cause) && this.l9();
};
protoOf(JobSupport).g9 = function (cause) {
  var finalState = get_COMPLETING_ALREADY();
  if (this.d9()) {
    finalState = cancelMakeCompleting(this, cause);
    if (finalState === get_COMPLETING_WAITING_CHILDREN())
      return true;
  }
  if (finalState === get_COMPLETING_ALREADY()) {
    finalState = makeCancelling(this, cause);
  }
  var tmp;
  if (finalState === get_COMPLETING_ALREADY()) {
    tmp = true;
  } else if (finalState === get_COMPLETING_WAITING_CHILDREN()) {
    tmp = true;
  } else if (finalState === get_TOO_LATE_TO_CANCEL()) {
    tmp = false;
  } else {
    this.k8(finalState);
    tmp = true;
  }
  return tmp;
};
protoOf(JobSupport).h9 = function () {
  var state = this.t8();
  var tmp;
  if (state instanceof Finishing) {
    tmp = state.ae();
  } else {
    if (state instanceof CompletedExceptionally) {
      tmp = state.f8_1;
    } else {
      if (!(state == null) ? isInterface(state, Incomplete) : false) {
        var message = 'Cannot be cancelling child in this state: ' + toString_0(state);
        throw IllegalStateException_init_$Create$_0(toString_1(message));
      } else {
        tmp = null;
      }
    }
  }
  var rootCause = tmp;
  var tmp1_elvis_lhs = rootCause instanceof CancellationException ? rootCause : null;
  return tmp1_elvis_lhs == null ? new JobCancellationException('Parent job is ' + stateString(this, state), rootCause, this) : tmp1_elvis_lhs;
};
protoOf(JobSupport).i8 = function (proposedUpdate) {
  // Inline function 'kotlinx.coroutines.JobSupport.loopOnState' call
  while (true) {
    var tmp0 = this.t8();
    $l$block: {
      var finalState = tryMakeCompleting(this, tmp0, proposedUpdate);
      if (finalState === get_COMPLETING_ALREADY())
        throw IllegalStateException_init_$Create$_1('Job ' + this.toString() + ' is already complete or completing, ' + ('but is being completed with ' + toString_0(proposedUpdate)), _get_exceptionOrNull__b3j7js(this, proposedUpdate));
      else if (finalState === get_COMPLETING_RETRY()) {
        break $l$block;
      } else
        return finalState;
    }
  }
};
protoOf(JobSupport).i9 = function (child) {
  // Inline function 'kotlinx.coroutines.asHandler' call
  // Inline function 'kotlin.js.asDynamic' call
  var tmp$ret$1 = new ChildHandleNode(child);
  var tmp = this.b9(true, VOID, tmp$ret$1);
  return isInterface(tmp, ChildHandle) ? tmp : THROW_CCE();
};
protoOf(JobSupport).l8 = function (exception) {
  throw exception;
};
protoOf(JobSupport).j9 = function (cause) {
};
protoOf(JobSupport).k9 = function () {
  return false;
};
protoOf(JobSupport).l9 = function () {
  return true;
};
protoOf(JobSupport).m9 = function (exception) {
  return false;
};
protoOf(JobSupport).e8 = function (state) {
};
protoOf(JobSupport).k8 = function (state) {
};
protoOf(JobSupport).toString = function () {
  return this.n9() + '@' + get_hexAddress(this);
};
protoOf(JobSupport).n9 = function () {
  return this.m8() + '{' + stateString(this, this.t8()) + '}';
};
protoOf(JobSupport).m8 = function () {
  return get_classSimpleName(this);
};
function boxIncomplete(_this__u8e3s4) {
  _init_properties_JobSupport_kt__68f172();
  var tmp;
  if (!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Incomplete) : false) {
    tmp = new IncompleteStateBox(_this__u8e3s4);
  } else {
    tmp = _this__u8e3s4;
  }
  return tmp;
}
function JobCancellingNode() {
  JobNode.call(this);
}
function InactiveNodeList(list) {
  this.wd_1 = list;
}
protoOf(InactiveNodeList).wc = function () {
  return this.wd_1;
};
protoOf(InactiveNodeList).a8 = function () {
  return false;
};
protoOf(InactiveNodeList).toString = function () {
  return get_DEBUG() ? this.wd_1.ad('New') : anyToString(this);
};
function ChildHandleNode(childJob) {
  JobCancellingNode.call(this);
  this.ge_1 = childJob;
}
protoOf(ChildHandleNode).qe = function (cause) {
  return this.ge_1.e9(this.od());
};
protoOf(ChildHandleNode).invoke = function (cause) {
  return this.qe(cause);
};
protoOf(ChildHandleNode).f9 = function (cause) {
  return this.od().f9(cause);
};
function InvokeOnCancelling(handler) {
  JobCancellingNode.call(this);
  this.we_1 = handler;
  this.xe_1 = atomic$int$1(0);
}
protoOf(InvokeOnCancelling).qe = function (cause) {
  if (this.xe_1.atomicfu$compareAndSet(0, 1))
    this.we_1(cause);
};
protoOf(InvokeOnCancelling).invoke = function (cause) {
  return this.qe(cause);
};
function InvokeOnCompletion(handler) {
  JobNode.call(this);
  this.cf_1 = handler;
}
protoOf(InvokeOnCompletion).qe = function (cause) {
  return this.cf_1(cause);
};
protoOf(InvokeOnCompletion).invoke = function (cause) {
  return this.qe(cause);
};
function IncompleteStateBox(state) {
  this.df_1 = state;
}
function ChildContinuation(child) {
  JobCancellingNode.call(this);
  this.if_1 = child;
}
protoOf(ChildContinuation).qe = function (cause) {
  this.if_1.hb(this.if_1.ib(this.od()));
};
protoOf(ChildContinuation).invoke = function (cause) {
  return this.qe(cause);
};
function handlesExceptionF($this) {
  var tmp = $this.s8();
  var tmp0_safe_receiver = tmp instanceof ChildHandleNode ? tmp : null;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.od();
  var tmp_0;
  if (tmp1_elvis_lhs == null) {
    return false;
  } else {
    tmp_0 = tmp1_elvis_lhs;
  }
  var parentJob = tmp_0;
  while (true) {
    if (parentJob.l9())
      return true;
    var tmp_1 = parentJob.s8();
    var tmp2_safe_receiver = tmp_1 instanceof ChildHandleNode ? tmp_1 : null;
    var tmp3_elvis_lhs = tmp2_safe_receiver == null ? null : tmp2_safe_receiver.od();
    var tmp_2;
    if (tmp3_elvis_lhs == null) {
      return false;
    } else {
      tmp_2 = tmp3_elvis_lhs;
    }
    parentJob = tmp_2;
  }
}
function JobImpl(parent) {
  JobSupport.call(this, true);
  this.v7(parent);
  this.lf_1 = handlesExceptionF(this);
}
protoOf(JobImpl).d9 = function () {
  return true;
};
protoOf(JobImpl).l9 = function () {
  return this.lf_1;
};
var properties_initialized_JobSupport_kt_5iq8a4;
function _init_properties_JobSupport_kt__68f172() {
  if (!properties_initialized_JobSupport_kt_5iq8a4) {
    properties_initialized_JobSupport_kt_5iq8a4 = true;
    COMPLETING_ALREADY = new Symbol_0('COMPLETING_ALREADY');
    COMPLETING_WAITING_CHILDREN = new Symbol_0('COMPLETING_WAITING_CHILDREN');
    COMPLETING_RETRY = new Symbol_0('COMPLETING_RETRY');
    TOO_LATE_TO_CANCEL = new Symbol_0('TOO_LATE_TO_CANCEL');
    SEALED = new Symbol_0('SEALED');
    EMPTY_NEW = new Empty(false);
    EMPTY_ACTIVE = new Empty(true);
  }
}
function MainCoroutineDispatcher() {
  CoroutineDispatcher.call(this);
}
protoOf(MainCoroutineDispatcher).toString = function () {
  var tmp0_elvis_lhs = this.of();
  return tmp0_elvis_lhs == null ? get_classSimpleName(this) + '@' + get_hexAddress(this) : tmp0_elvis_lhs;
};
protoOf(MainCoroutineDispatcher).of = function () {
  var main = Dispatchers_getInstance().ec();
  if (this === main)
    return 'Dispatchers.Main';
  var tmp;
  try {
    tmp = main.nf();
  } catch ($p) {
    var tmp_0;
    if ($p instanceof UnsupportedOperationException) {
      var e = $p;
      tmp_0 = null;
    } else {
      throw $p;
    }
    tmp = tmp_0;
  }
  var immediate = tmp;
  if (this === immediate)
    return 'Dispatchers.Main.immediate';
  return null;
};
function SupervisorJob(parent) {
  parent = parent === VOID ? null : parent;
  return new SupervisorJobImpl(parent);
}
function SupervisorJobImpl(parent) {
  JobImpl.call(this, parent);
}
protoOf(SupervisorJobImpl).f9 = function (cause) {
  return false;
};
function TimeoutCancellationException() {
}
function Unconfined() {
  Unconfined_instance = this;
  CoroutineDispatcher.call(this);
}
protoOf(Unconfined).wb = function (context) {
  return false;
};
protoOf(Unconfined).xb = function (context, block) {
  var yieldContext = context.m4(Key_instance_3);
  if (!(yieldContext == null)) {
    yieldContext.uf_1 = true;
    return Unit_instance;
  }
  throw UnsupportedOperationException_init_$Create$_0('Dispatchers.Unconfined.dispatch function can only be used by the yield function. If you wrap Unconfined dispatcher in your code, make sure you properly delegate isDispatchNeeded and dispatch calls.');
};
protoOf(Unconfined).toString = function () {
  return 'Dispatchers.Unconfined';
};
var Unconfined_instance;
function Unconfined_getInstance() {
  if (Unconfined_instance == null)
    new Unconfined();
  return Unconfined_instance;
}
function Key_3() {
}
var Key_instance_3;
function Key_getInstance_3() {
  return Key_instance_3;
}
function OpDescriptor() {
}
function Segment() {
}
function ConcurrentLinkedListNode() {
}
function handleUncaughtCoroutineException(context, exception) {
  var tmp0_iterator = get_platformExceptionHandlers().c();
  while (tmp0_iterator.d()) {
    var handler = tmp0_iterator.e();
    try {
      handler.zb(context, exception);
    } catch ($p) {
      if ($p instanceof ExceptionSuccessfullyProcessed) {
        var _ = $p;
        return Unit_instance;
      } else {
        if ($p instanceof Error) {
          var t = $p;
          propagateExceptionFinalResort(handlerException(exception, t));
        } else {
          throw $p;
        }
      }
    }
  }
  try {
    addSuppressed(exception, new DiagnosticCoroutineContextException(context));
  } catch ($p) {
    if ($p instanceof Error) {
      var e = $p;
    } else {
      throw $p;
    }
  }
  propagateExceptionFinalResort(exception);
}
function ExceptionSuccessfullyProcessed() {
}
function get_UNDEFINED() {
  _init_properties_DispatchedContinuation_kt__tnmqc0();
  return UNDEFINED;
}
var UNDEFINED;
function get_REUSABLE_CLAIMED() {
  _init_properties_DispatchedContinuation_kt__tnmqc0();
  return REUSABLE_CLAIMED;
}
var REUSABLE_CLAIMED;
function _get_reusableCancellableContinuation__9qex09($this) {
  var tmp = $this.ha_1.kotlinx$atomicfu$value;
  return tmp instanceof CancellableContinuationImpl ? tmp : null;
}
function DispatchedContinuation(dispatcher, continuation) {
  DispatchedTask.call(this, -1);
  this.da_1 = dispatcher;
  this.ea_1 = continuation;
  this.fa_1 = get_UNDEFINED();
  this.ga_1 = threadContextElements(this.f4());
  this.ha_1 = atomic$ref$1(null);
}
protoOf(DispatchedContinuation).f4 = function () {
  return this.ea_1.f4();
};
protoOf(DispatchedContinuation).ia = function () {
  return !(this.ha_1.kotlinx$atomicfu$value == null);
};
protoOf(DispatchedContinuation).vf = function () {
  // Inline function 'kotlinx.atomicfu.loop' call
  var this_0 = this.ha_1;
  while (true) {
    if (!(this_0.kotlinx$atomicfu$value === get_REUSABLE_CLAIMED()))
      return Unit_instance;
  }
};
protoOf(DispatchedContinuation).yb = function () {
  this.vf();
  var tmp0_safe_receiver = _get_reusableCancellableContinuation__9qex09(this);
  if (tmp0_safe_receiver == null)
    null;
  else {
    tmp0_safe_receiver.ra();
  }
};
protoOf(DispatchedContinuation).mb = function (continuation) {
  // Inline function 'kotlinx.atomicfu.loop' call
  var this_0 = this.ha_1;
  while (true) {
    var state = this_0.kotlinx$atomicfu$value;
    if (state === get_REUSABLE_CLAIMED()) {
      if (this.ha_1.atomicfu$compareAndSet(get_REUSABLE_CLAIMED(), continuation))
        return null;
    } else {
      if (state instanceof Error) {
        // Inline function 'kotlin.require' call
        // Inline function 'kotlin.require' call
        if (!this.ha_1.atomicfu$compareAndSet(state, null)) {
          var message = 'Failed requirement.';
          throw IllegalArgumentException_init_$Create$_0(toString_1(message));
        }
        return state;
      } else {
        // Inline function 'kotlin.error' call
        var message_0 = 'Inconsistent state ' + toString_0(state);
        throw IllegalStateException_init_$Create$_0(toString_1(message_0));
      }
    }
  }
};
protoOf(DispatchedContinuation).ka = function (cause) {
  // Inline function 'kotlinx.atomicfu.loop' call
  var this_0 = this.ha_1;
  while (true) {
    var state = this_0.kotlinx$atomicfu$value;
    if (equals(state, get_REUSABLE_CLAIMED())) {
      if (this.ha_1.atomicfu$compareAndSet(get_REUSABLE_CLAIMED(), cause))
        return true;
    } else {
      if (state instanceof Error)
        return true;
      else {
        if (this.ha_1.atomicfu$compareAndSet(state, null))
          return false;
      }
    }
  }
};
protoOf(DispatchedContinuation).va = function () {
  var state = this.fa_1;
  // Inline function 'kotlinx.coroutines.assert' call
  this.fa_1 = get_UNDEFINED();
  return state;
};
protoOf(DispatchedContinuation).sa = function () {
  return this;
};
protoOf(DispatchedContinuation).k4 = function (result) {
  var context = this.ea_1.f4();
  var state = toState_0(result);
  if (this.da_1.wb(context)) {
    this.fa_1 = state;
    this.ja_1 = 0;
    this.da_1.xb(context, this);
  } else {
    $l$block: {
      // Inline function 'kotlinx.coroutines.internal.executeUnconfined' call
      // Inline function 'kotlinx.coroutines.assert' call
      var eventLoop = ThreadLocalEventLoop_getInstance().rc();
      if (false && eventLoop.mc()) {
        break $l$block;
      }
      var tmp;
      if (eventLoop.lc()) {
        this.fa_1 = state;
        this.ja_1 = 0;
        eventLoop.kc(this);
        tmp = true;
      } else {
        // Inline function 'kotlinx.coroutines.runUnconfinedEventLoop' call
        eventLoop.nc(true);
        try {
          this.f4();
          // Inline function 'kotlinx.coroutines.withCoroutineContext' call
          this.ga_1;
          this.ea_1.k4(result);
          $l$loop: while (eventLoop.jc()) {
          }
        } catch ($p) {
          if ($p instanceof Error) {
            var e = $p;
            this.qb(e, null);
          } else {
            throw $p;
          }
        }
        finally {
          eventLoop.oc(true);
        }
        tmp = false;
      }
    }
  }
};
protoOf(DispatchedContinuation).wa = function (takenState, cause) {
  if (takenState instanceof CompletedWithCancellation) {
    takenState.ub_1(cause);
  }
};
protoOf(DispatchedContinuation).toString = function () {
  return 'DispatchedContinuation[' + this.da_1.toString() + ', ' + toDebugString(this.ea_1) + ']';
};
function resumeCancellableWith(_this__u8e3s4, result, onCancellation) {
  onCancellation = onCancellation === VOID ? null : onCancellation;
  _init_properties_DispatchedContinuation_kt__tnmqc0();
  var tmp;
  if (_this__u8e3s4 instanceof DispatchedContinuation) {
    // Inline function 'kotlinx.coroutines.internal.DispatchedContinuation.resumeCancellableWith' call
    var state = toState_0(result, onCancellation);
    if (_this__u8e3s4.da_1.wb(_this__u8e3s4.f4())) {
      _this__u8e3s4.fa_1 = state;
      _this__u8e3s4.ja_1 = 1;
      _this__u8e3s4.da_1.xb(_this__u8e3s4.f4(), _this__u8e3s4);
    } else {
      $l$block: {
        // Inline function 'kotlinx.coroutines.internal.executeUnconfined' call
        // Inline function 'kotlinx.coroutines.assert' call
        var eventLoop = ThreadLocalEventLoop_getInstance().rc();
        if (false && eventLoop.mc()) {
          break $l$block;
        }
        var tmp_0;
        if (eventLoop.lc()) {
          _this__u8e3s4.fa_1 = state;
          _this__u8e3s4.ja_1 = 1;
          eventLoop.kc(_this__u8e3s4);
          tmp_0 = true;
        } else {
          // Inline function 'kotlinx.coroutines.runUnconfinedEventLoop' call
          eventLoop.nc(true);
          try {
            var tmp$ret$4;
            $l$block_0: {
              // Inline function 'kotlinx.coroutines.internal.DispatchedContinuation.resumeCancelled' call
              var job = _this__u8e3s4.f4().m4(Key_instance_2);
              if (!(job == null) && !job.a8()) {
                var cause = job.x8();
                _this__u8e3s4.wa(state, cause);
                // Inline function 'kotlin.coroutines.resumeWithException' call
                // Inline function 'kotlin.Companion.failure' call
                var tmp$ret$2 = _Result___init__impl__xyqfz8(createFailure(cause));
                _this__u8e3s4.k4(tmp$ret$2);
                tmp$ret$4 = true;
                break $l$block_0;
              }
              tmp$ret$4 = false;
            }
            if (!tmp$ret$4) {
              // Inline function 'kotlinx.coroutines.internal.DispatchedContinuation.resumeUndispatchedWith' call
              _this__u8e3s4.ea_1;
              // Inline function 'kotlinx.coroutines.withContinuationContext' call
              _this__u8e3s4.ga_1;
              _this__u8e3s4.ea_1.k4(result);
            }
            $l$loop: while (eventLoop.jc()) {
            }
          } catch ($p) {
            if ($p instanceof Error) {
              var e = $p;
              _this__u8e3s4.qb(e, null);
            } else {
              throw $p;
            }
          }
          finally {
            eventLoop.oc(true);
          }
          tmp_0 = false;
        }
      }
    }
    tmp = Unit_instance;
  } else {
    _this__u8e3s4.k4(result);
    tmp = Unit_instance;
  }
  return tmp;
}
var properties_initialized_DispatchedContinuation_kt_2siadq;
function _init_properties_DispatchedContinuation_kt__tnmqc0() {
  if (!properties_initialized_DispatchedContinuation_kt_2siadq) {
    properties_initialized_DispatchedContinuation_kt_2siadq = true;
    UNDEFINED = new Symbol_0('UNDEFINED');
    REUSABLE_CLAIMED = new Symbol_0('REUSABLE_CLAIMED');
  }
}
function DispatchedTask(resumeMode) {
  SchedulerTask.call(this);
  this.ja_1 = resumeMode;
}
protoOf(DispatchedTask).wa = function (takenState, cause) {
};
protoOf(DispatchedTask).lb = function (state) {
  return (state == null ? true : !(state == null)) ? state : THROW_CCE();
};
protoOf(DispatchedTask).nb = function (state) {
  var tmp0_safe_receiver = state instanceof CompletedExceptionally ? state : null;
  return tmp0_safe_receiver == null ? null : tmp0_safe_receiver.f8_1;
};
protoOf(DispatchedTask).pb = function () {
  // Inline function 'kotlinx.coroutines.assert' call
  var taskContext = get_taskContext(this);
  var fatalException = null;
  try {
    var tmp = this.sa();
    var delegate = tmp instanceof DispatchedContinuation ? tmp : THROW_CCE();
    var continuation = delegate.ea_1;
    // Inline function 'kotlinx.coroutines.withContinuationContext' call
    delegate.ga_1;
    var context = continuation.f4();
    var state = this.va();
    var exception = this.nb(state);
    var job = exception == null && get_isCancellableMode(this.ja_1) ? context.m4(Key_instance_2) : null;
    if (!(job == null) && !job.a8()) {
      var cause = job.x8();
      this.wa(state, cause);
      // Inline function 'kotlinx.coroutines.resumeWithStackTrace' call
      // Inline function 'kotlin.Companion.failure' call
      var exception_0 = recoverStackTrace(cause, continuation);
      var tmp$ret$1 = _Result___init__impl__xyqfz8(createFailure(exception_0));
      continuation.k4(tmp$ret$1);
    } else {
      if (!(exception == null)) {
        // Inline function 'kotlin.coroutines.resumeWithException' call
        // Inline function 'kotlin.Companion.failure' call
        var tmp$ret$3 = _Result___init__impl__xyqfz8(createFailure(exception));
        continuation.k4(tmp$ret$3);
      } else {
        // Inline function 'kotlin.coroutines.resume' call
        // Inline function 'kotlin.Companion.success' call
        var value = this.lb(state);
        var tmp$ret$5 = _Result___init__impl__xyqfz8(value);
        continuation.k4(tmp$ret$5);
      }
    }
  } catch ($p) {
    if ($p instanceof Error) {
      var e = $p;
      fatalException = e;
    } else {
      throw $p;
    }
  }
  finally {
    // Inline function 'kotlin.runCatching' call
    var tmp_0;
    try {
      // Inline function 'kotlinx.coroutines.afterTask' call
      // Inline function 'kotlin.Companion.success' call
      tmp_0 = _Result___init__impl__xyqfz8(Unit_instance);
    } catch ($p) {
      var tmp_1;
      if ($p instanceof Error) {
        var e_0 = $p;
        // Inline function 'kotlin.Companion.failure' call
        tmp_1 = _Result___init__impl__xyqfz8(createFailure(e_0));
      } else {
        throw $p;
      }
      tmp_0 = tmp_1;
    }
    var result = tmp_0;
    this.qb(fatalException, Result__exceptionOrNull_impl_p6xea9(result));
  }
};
protoOf(DispatchedTask).qb = function (exception, finallyException) {
  if (exception === null && finallyException === null)
    return Unit_instance;
  if (!(exception === null) && !(finallyException === null)) {
    addSuppressed(exception, finallyException);
  }
  var cause = exception == null ? finallyException : exception;
  var reason = new CoroutinesInternalError('Fatal exception in coroutines machinery for ' + toString_1(this) + '. ' + "Please read KDoc to 'handleFatalException' method and report this incident to maintainers", ensureNotNull(cause));
  handleCoroutineException(this.sa().f4(), reason);
};
function get_isReusableMode(_this__u8e3s4) {
  return _this__u8e3s4 === 2;
}
function get_isCancellableMode(_this__u8e3s4) {
  return _this__u8e3s4 === 1 || _this__u8e3s4 === 2;
}
function dispatch(_this__u8e3s4, mode) {
  // Inline function 'kotlinx.coroutines.assert' call
  var delegate = _this__u8e3s4.sa();
  var undispatched = mode === 4;
  var tmp;
  var tmp_0;
  if (!undispatched) {
    tmp_0 = delegate instanceof DispatchedContinuation;
  } else {
    tmp_0 = false;
  }
  if (tmp_0) {
    tmp = get_isCancellableMode(mode) === get_isCancellableMode(_this__u8e3s4.ja_1);
  } else {
    tmp = false;
  }
  if (tmp) {
    var dispatcher = delegate.da_1;
    var context = delegate.f4();
    if (dispatcher.wb(context)) {
      dispatcher.xb(context, _this__u8e3s4);
    } else {
      resumeUnconfined(_this__u8e3s4);
    }
  } else {
    resume(_this__u8e3s4, delegate, undispatched);
  }
}
function resumeUnconfined(_this__u8e3s4) {
  var eventLoop = ThreadLocalEventLoop_getInstance().rc();
  if (eventLoop.lc()) {
    eventLoop.kc(_this__u8e3s4);
  } else {
    // Inline function 'kotlinx.coroutines.runUnconfinedEventLoop' call
    eventLoop.nc(true);
    try {
      resume(_this__u8e3s4, _this__u8e3s4.sa(), true);
      $l$loop: while (eventLoop.jc()) {
      }
    } catch ($p) {
      if ($p instanceof Error) {
        var e = $p;
        _this__u8e3s4.qb(e, null);
      } else {
        throw $p;
      }
    }
    finally {
      eventLoop.oc(true);
    }
  }
}
function resume(_this__u8e3s4, delegate, undispatched) {
  var state = _this__u8e3s4.va();
  var exception = _this__u8e3s4.nb(state);
  var tmp;
  if (!(exception == null)) {
    // Inline function 'kotlin.Companion.failure' call
    tmp = _Result___init__impl__xyqfz8(createFailure(exception));
  } else {
    // Inline function 'kotlin.Companion.success' call
    var value = _this__u8e3s4.lb(state);
    tmp = _Result___init__impl__xyqfz8(value);
  }
  var result = tmp;
  if (undispatched) {
    // Inline function 'kotlinx.coroutines.internal.DispatchedContinuation.resumeUndispatchedWith' call
    var this_0 = delegate instanceof DispatchedContinuation ? delegate : THROW_CCE();
    this_0.ea_1;
    // Inline function 'kotlinx.coroutines.withContinuationContext' call
    this_0.ga_1;
    this_0.ea_1.k4(result);
  } else {
    delegate.k4(result);
  }
}
function ContextScope(context) {
  this.wf_1 = context;
}
protoOf(ContextScope).z7 = function () {
  return this.wf_1;
};
protoOf(ContextScope).toString = function () {
  return 'CoroutineScope(coroutineContext=' + toString_1(this.wf_1) + ')';
};
function Symbol_0(symbol) {
  this.xf_1 = symbol;
}
protoOf(Symbol_0).toString = function () {
  return '<' + this.xf_1 + '>';
};
function startCoroutineCancellable(_this__u8e3s4, fatalCompletion) {
  // Inline function 'kotlinx.coroutines.intrinsics.runSafely' call
  try {
    var tmp = intercepted(_this__u8e3s4);
    // Inline function 'kotlin.Companion.success' call
    var tmp$ret$0 = _Result___init__impl__xyqfz8(Unit_instance);
    resumeCancellableWith(tmp, tmp$ret$0);
  } catch ($p) {
    if ($p instanceof Error) {
      var e = $p;
      dispatcherFailure(fatalCompletion, e);
    } else {
      throw $p;
    }
  }
  return Unit_instance;
}
function startCoroutineCancellable_0(_this__u8e3s4, receiver, completion, onCancellation) {
  onCancellation = onCancellation === VOID ? null : onCancellation;
  // Inline function 'kotlinx.coroutines.intrinsics.runSafely' call
  try {
    var tmp = intercepted(createCoroutineUnintercepted(_this__u8e3s4, receiver, completion));
    // Inline function 'kotlin.Companion.success' call
    var tmp$ret$0 = _Result___init__impl__xyqfz8(Unit_instance);
    resumeCancellableWith(tmp, tmp$ret$0, onCancellation);
  } catch ($p) {
    if ($p instanceof Error) {
      var e = $p;
      dispatcherFailure(completion, e);
    } else {
      throw $p;
    }
  }
  return Unit_instance;
}
function dispatcherFailure(completion, e) {
  // Inline function 'kotlin.Companion.failure' call
  var tmp$ret$0 = _Result___init__impl__xyqfz8(createFailure(e));
  completion.k4(tmp$ret$0);
  throw e;
}
function startCoroutineUndispatched(_this__u8e3s4, receiver, completion) {
  $l$block: {
    // Inline function 'kotlinx.coroutines.intrinsics.startDirect' call
    // Inline function 'kotlinx.coroutines.internal.probeCoroutineCreated' call
    var actualCompletion = completion;
    var tmp;
    try {
      // Inline function 'kotlinx.coroutines.withCoroutineContext' call
      completion.f4();
      // Inline function 'kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn' call
      tmp = startCoroutineUninterceptedOrReturnNonGeneratorVersion(_this__u8e3s4, receiver, actualCompletion);
    } catch ($p) {
      var tmp_0;
      if ($p instanceof Error) {
        var e = $p;
        // Inline function 'kotlin.coroutines.resumeWithException' call
        // Inline function 'kotlin.Companion.failure' call
        var tmp$ret$5 = _Result___init__impl__xyqfz8(createFailure(e));
        actualCompletion.k4(tmp$ret$5);
        break $l$block;
      } else {
        throw $p;
      }
    }
    var value = tmp;
    if (!(value === get_COROUTINE_SUSPENDED())) {
      // Inline function 'kotlin.coroutines.resume' call
      // Inline function 'kotlin.Companion.success' call
      var value_0 = (value == null ? true : !(value == null)) ? value : THROW_CCE();
      var tmp$ret$8 = _Result___init__impl__xyqfz8(value_0);
      actualCompletion.k4(tmp$ret$8);
    }
  }
}
function CompletionHandlerBase() {
  LinkedListNode.call(this);
}
function invokeIt(_this__u8e3s4, cause) {
  if (typeof _this__u8e3s4 === 'function')
    _this__u8e3s4(cause);
  else {
    // Inline function 'kotlin.js.asDynamic' call
    _this__u8e3s4.invoke(cause);
  }
}
function CancelHandlerBase() {
}
function toDebugString(_this__u8e3s4) {
  return toString_1(_this__u8e3s4);
}
function createDefaultDispatcher() {
  var tmp;
  if (isJsdom()) {
    tmp = NodeDispatcher_getInstance();
  } else {
    var tmp_0;
    var tmp_1;
    if (!(typeof window === 'undefined')) {
      // Inline function 'kotlin.js.asDynamic' call
      tmp_1 = window != null;
    } else {
      tmp_1 = false;
    }
    if (tmp_1) {
      // Inline function 'kotlin.js.asDynamic' call
      tmp_0 = !(typeof window.addEventListener === 'undefined');
    } else {
      tmp_0 = false;
    }
    if (tmp_0) {
      tmp = asCoroutineDispatcher(window);
    } else {
      if (typeof process === 'undefined' || typeof process.nextTick === 'undefined') {
        tmp = SetTimeoutDispatcher_getInstance();
      } else {
        tmp = NodeDispatcher_getInstance();
      }
    }
  }
  return tmp;
}
function isJsdom() {
  return !(typeof navigator === 'undefined') && navigator != null && navigator.userAgent != null && !(typeof navigator.userAgent === 'undefined') && !(typeof navigator.userAgent.match === 'undefined') && navigator.userAgent.match('\\bjsdom\\b');
}
function newCoroutineContext(_this__u8e3s4, context) {
  var combined = _this__u8e3s4.z7().i7(context);
  return !(combined === Dispatchers_getInstance().ac_1) && combined.m4(Key_instance) == null ? combined.i7(Dispatchers_getInstance().ac_1) : combined;
}
function get_coroutineName(_this__u8e3s4) {
  return null;
}
var counter;
function get_DEBUG() {
  return DEBUG;
}
var DEBUG;
function get_classSimpleName(_this__u8e3s4) {
  var tmp0_elvis_lhs = getKClassFromExpression(_this__u8e3s4).l5();
  return tmp0_elvis_lhs == null ? 'Unknown' : tmp0_elvis_lhs;
}
function get_hexAddress(_this__u8e3s4) {
  // Inline function 'kotlin.js.asDynamic' call
  var result = _this__u8e3s4.__debug_counter;
  if (!(typeof result === 'number')) {
    counter = counter + 1 | 0;
    result = counter;
    // Inline function 'kotlin.js.asDynamic' call
    _this__u8e3s4.__debug_counter = result;
  }
  return ((!(result == null) ? typeof result === 'number' : false) ? result : THROW_CCE()).toString();
}
function NodeDispatcher() {
  NodeDispatcher_instance = this;
  SetTimeoutBasedDispatcher.call(this);
}
protoOf(NodeDispatcher).ag = function () {
  process.nextTick(this.hg_1.fg_1);
};
var NodeDispatcher_instance;
function NodeDispatcher_getInstance() {
  if (NodeDispatcher_instance == null)
    new NodeDispatcher();
  return NodeDispatcher_instance;
}
function ScheduledMessageQueue$processQueue$lambda(this$0) {
  return function () {
    this$0.lg();
    return Unit_instance;
  };
}
function ScheduledMessageQueue(dispatcher) {
  MessageQueue.call(this);
  this.eg_1 = dispatcher;
  var tmp = this;
  tmp.fg_1 = ScheduledMessageQueue$processQueue$lambda(this);
}
protoOf(ScheduledMessageQueue).mg = function () {
  this.eg_1.ag();
};
protoOf(ScheduledMessageQueue).ng = function () {
  setTimeout(this.fg_1, 0);
};
protoOf(ScheduledMessageQueue).og = function (timeout) {
  setTimeout(this.fg_1, timeout);
};
function WindowMessageQueue$lambda(this$0) {
  return function (event) {
    var tmp;
    if (event.source == this$0.ug_1 && event.data == this$0.vg_1) {
      event.stopPropagation();
      this$0.lg();
      tmp = Unit_instance;
    }
    return Unit_instance;
  };
}
function WindowMessageQueue$schedule$lambda(this$0) {
  return function (it) {
    this$0.lg();
    return Unit_instance;
  };
}
function WindowMessageQueue(window_0) {
  MessageQueue.call(this);
  this.ug_1 = window_0;
  this.vg_1 = 'dispatchCoroutine';
  this.ug_1.addEventListener('message', WindowMessageQueue$lambda(this), true);
}
protoOf(WindowMessageQueue).mg = function () {
  var tmp = Promise.resolve(Unit_instance);
  tmp.then(WindowMessageQueue$schedule$lambda(this));
};
protoOf(WindowMessageQueue).ng = function () {
  this.ug_1.postMessage(this.vg_1, '*');
};
function await_0(_this__u8e3s4, $completion) {
  var cancellable = new CancellableContinuationImpl(intercepted($completion), 1);
  cancellable.ta();
  var tmp = await$lambda(cancellable);
  _this__u8e3s4.then(tmp, await$lambda_0(cancellable));
  return cancellable.jb();
}
function await$lambda($cont) {
  return function (it) {
    // Inline function 'kotlin.coroutines.resume' call
    var this_0 = $cont;
    // Inline function 'kotlin.Companion.success' call
    var tmp$ret$0 = _Result___init__impl__xyqfz8(it);
    this_0.k4(tmp$ret$0);
    return Unit_instance;
  };
}
function await$lambda_0($cont) {
  return function (it) {
    // Inline function 'kotlin.coroutines.resumeWithException' call
    var this_0 = $cont;
    // Inline function 'kotlin.Companion.failure' call
    var tmp$ret$0 = _Result___init__impl__xyqfz8(createFailure(it));
    this_0.k4(tmp$ret$0);
    return Unit_instance;
  };
}
function asCoroutineDispatcher(_this__u8e3s4) {
  // Inline function 'kotlin.js.asDynamic' call
  var tmp0_elvis_lhs = _this__u8e3s4.coroutineDispatcher;
  var tmp;
  if (tmp0_elvis_lhs == null) {
    // Inline function 'kotlin.also' call
    var this_0 = new WindowDispatcher(_this__u8e3s4);
    // Inline function 'kotlin.js.asDynamic' call
    _this__u8e3s4.coroutineDispatcher = this_0;
    tmp = this_0;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  return tmp;
}
function propagateExceptionFinalResort(exception) {
  console.error(exception.toString());
}
function Dispatchers() {
  Dispatchers_instance = this;
  this.ac_1 = createDefaultDispatcher();
  this.bc_1 = Unconfined_getInstance();
  this.cc_1 = new JsMainDispatcher(this.ac_1, false);
  this.dc_1 = null;
}
protoOf(Dispatchers).ec = function () {
  var tmp0_elvis_lhs = this.dc_1;
  return tmp0_elvis_lhs == null ? this.cc_1 : tmp0_elvis_lhs;
};
var Dispatchers_instance;
function Dispatchers_getInstance() {
  if (Dispatchers_instance == null)
    new Dispatchers();
  return Dispatchers_instance;
}
function JsMainDispatcher(delegate, invokeImmediately) {
  MainCoroutineDispatcher.call(this);
  this.xg_1 = delegate;
  this.yg_1 = invokeImmediately;
  this.zg_1 = this.yg_1 ? this : new JsMainDispatcher(this.xg_1, true);
}
protoOf(JsMainDispatcher).nf = function () {
  return this.zg_1;
};
protoOf(JsMainDispatcher).wb = function (context) {
  return !this.yg_1;
};
protoOf(JsMainDispatcher).xb = function (context, block) {
  return this.xg_1.xb(context, block);
};
protoOf(JsMainDispatcher).toString = function () {
  var tmp0_elvis_lhs = this.of();
  return tmp0_elvis_lhs == null ? this.xg_1.toString() : tmp0_elvis_lhs;
};
function createEventLoop() {
  return new UnconfinedEventLoop();
}
function UnconfinedEventLoop() {
  EventLoop.call(this);
}
protoOf(UnconfinedEventLoop).xb = function (context, block) {
  unsupported();
};
function unsupported() {
  throw UnsupportedOperationException_init_$Create$_0('runBlocking event loop is not supported');
}
function JobCancellationException(message, cause, job) {
  CancellationException_init_$Init$_1(message, cause, this);
  captureStack(this, JobCancellationException);
  this.eh_1 = job;
}
protoOf(JobCancellationException).toString = function () {
  return protoOf(CancellationException).toString.call(this) + '; job=' + toString_1(this.eh_1);
};
protoOf(JobCancellationException).equals = function (other) {
  var tmp;
  if (other === this) {
    tmp = true;
  } else {
    var tmp_0;
    var tmp_1;
    var tmp_2;
    if (other instanceof JobCancellationException) {
      tmp_2 = other.message == this.message;
    } else {
      tmp_2 = false;
    }
    if (tmp_2) {
      tmp_1 = equals(other.eh_1, this.eh_1);
    } else {
      tmp_1 = false;
    }
    if (tmp_1) {
      tmp_0 = equals(other.cause, this.cause);
    } else {
      tmp_0 = false;
    }
    tmp = tmp_0;
  }
  return tmp;
};
protoOf(JobCancellationException).hashCode = function () {
  var tmp = imul_0(imul_0(getStringHashCode(ensureNotNull(this.message)), 31) + hashCode_0(this.eh_1) | 0, 31);
  var tmp0_safe_receiver = this.cause;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : hashCode_0(tmp0_safe_receiver);
  return tmp + (tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs) | 0;
};
function Runnable() {
}
function SchedulerTask() {
}
function get_taskContext(_this__u8e3s4) {
  return TaskContext_instance;
}
function TaskContext() {
}
var TaskContext_instance;
function TaskContext_getInstance() {
  return TaskContext_instance;
}
function identitySet(expectedSize) {
  return HashSet_init_$Create$_0(expectedSize);
}
function get_platformExceptionHandlers_() {
  _init_properties_CoroutineExceptionHandlerImpl_kt__37d7wf();
  return platformExceptionHandlers_;
}
var platformExceptionHandlers_;
function get_platformExceptionHandlers() {
  _init_properties_CoroutineExceptionHandlerImpl_kt__37d7wf();
  return get_platformExceptionHandlers_();
}
function DiagnosticCoroutineContextException(context) {
  RuntimeException_init_$Init$_0(toString_1(context), this);
  captureStack(this, DiagnosticCoroutineContextException);
}
var properties_initialized_CoroutineExceptionHandlerImpl_kt_qhrgvx;
function _init_properties_CoroutineExceptionHandlerImpl_kt__37d7wf() {
  if (!properties_initialized_CoroutineExceptionHandlerImpl_kt_qhrgvx) {
    properties_initialized_CoroutineExceptionHandlerImpl_kt_qhrgvx = true;
    // Inline function 'kotlin.collections.mutableSetOf' call
    platformExceptionHandlers_ = LinkedHashSet_init_$Create$();
  }
}
function SetTimeoutDispatcher() {
  SetTimeoutDispatcher_instance = this;
  SetTimeoutBasedDispatcher.call(this);
}
protoOf(SetTimeoutDispatcher).ag = function () {
  this.hg_1.og(0);
};
var SetTimeoutDispatcher_instance;
function SetTimeoutDispatcher_getInstance() {
  if (SetTimeoutDispatcher_instance == null)
    new SetTimeoutDispatcher();
  return SetTimeoutDispatcher_instance;
}
function SetTimeoutBasedDispatcher() {
  CoroutineDispatcher.call(this);
  this.hg_1 = new ScheduledMessageQueue(this);
}
protoOf(SetTimeoutBasedDispatcher).xb = function (context, block) {
  this.hg_1.qg(block);
};
function MessageQueue() {
  this.ig_1 = ArrayDeque_init_$Create$();
  this.jg_1 = 16;
  this.kg_1 = false;
}
protoOf(MessageQueue).f = function () {
  return this.ig_1.t6_1;
};
protoOf(MessageQueue).pg = function (element) {
  return this.ig_1.b1(element);
};
protoOf(MessageQueue).b1 = function (element) {
  return this.pg((!(element == null) ? isInterface(element, Runnable) : false) ? element : THROW_CCE());
};
protoOf(MessageQueue).i = function (index) {
  return this.ig_1.i(index);
};
protoOf(MessageQueue).g = function () {
  return this.ig_1.g();
};
protoOf(MessageQueue).c = function () {
  return this.ig_1.c();
};
protoOf(MessageQueue).i1 = function (index) {
  return this.ig_1.i1(index);
};
protoOf(MessageQueue).qg = function (element) {
  this.pg(element);
  if (!this.kg_1) {
    this.kg_1 = true;
    this.mg();
  }
};
protoOf(MessageQueue).lg = function () {
  try {
    // Inline function 'kotlin.repeat' call
    var times = this.jg_1;
    var inductionVariable = 0;
    if (inductionVariable < times)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var tmp0_elvis_lhs = removeFirstOrNull(this);
        var tmp;
        if (tmp0_elvis_lhs == null) {
          return Unit_instance;
        } else {
          tmp = tmp0_elvis_lhs;
        }
        var element = tmp;
        element.pb();
      }
       while (inductionVariable < times);
  }finally {
    if (this.g()) {
      this.kg_1 = false;
    } else {
      this.ng();
    }
  }
};
function WindowDispatcher(window_0) {
  CoroutineDispatcher.call(this);
  this.ih_1 = window_0;
  this.jh_1 = new WindowMessageQueue(this.ih_1);
}
protoOf(WindowDispatcher).xb = function (context, block) {
  return this.jh_1.qg(block);
};
function LinkedListHead() {
  LinkedListNode.call(this);
}
protoOf(LinkedListHead).hd = function () {
  throw UnsupportedOperationException_init_$Create$();
};
function LinkedListNode() {
  this.bd_1 = this;
  this.cd_1 = this;
  this.dd_1 = false;
}
protoOf(LinkedListNode).id = function (node) {
  var prev = this.cd_1;
  node.bd_1 = this;
  node.cd_1 = prev;
  prev.bd_1 = node;
  this.cd_1 = node;
};
protoOf(LinkedListNode).hd = function () {
  return this.jd();
};
protoOf(LinkedListNode).ua = function () {
  this.hd();
};
protoOf(LinkedListNode).jd = function () {
  if (this.dd_1)
    return false;
  var prev = this.cd_1;
  var next = this.bd_1;
  prev.bd_1 = next;
  next.cd_1 = prev;
  this.dd_1 = true;
  return true;
};
protoOf(LinkedListNode).pd = function (node) {
  if (!(this.bd_1 === this))
    return false;
  this.id(node);
  return true;
};
function unwrap(exception) {
  return exception;
}
function recoverStackTrace(exception, continuation) {
  return exception;
}
function SynchronizedObject() {
}
function threadContextElements(context) {
  return 0;
}
function CommonThreadLocal() {
  this.sc_1 = null;
}
protoOf(CommonThreadLocal).tc = function () {
  var tmp = this.sc_1;
  return (tmp == null ? true : !(tmp == null)) ? tmp : THROW_CCE();
};
protoOf(CommonThreadLocal).uc = function (value) {
  this.sc_1 = value;
};
function commonThreadLocal(name) {
  return new CommonThreadLocal();
}
function WebRTCController$startListening$lambda$slambda$lambda$lambda$lambda(this$0, $answer) {
  return function () {
    sendSignal(this$0, $answer);
    return Unit_instance;
  };
}
function WebRTCController$startListening$lambda$slambda$lambda$lambda(this$0) {
  return function (answer) {
    return this$0.lh_1.setLocalDescription(answer).then(WebRTCController$startListening$lambda$slambda$lambda$lambda$lambda(this$0, answer));
  };
}
function WebRTCController$startListening$lambda$slambda$lambda(this$0) {
  return function () {
    return this$0.lh_1.createAnswer().then(WebRTCController$startListening$lambda$slambda$lambda$lambda(this$0));
  };
}
function setupP2P($this, $completion) {
  var tmp = new $setupP2PCOROUTINE$($this, $completion);
  tmp.b4_1 = Unit_instance;
  tmp.c4_1 = null;
  return tmp.h4();
}
function setDataChannel($this, channel) {
  $this.mh_1 = channel;
  $this.mh_1.onopen = WebRTCController$setDataChannel$lambda;
  $this.mh_1.onmessage = WebRTCController$setDataChannel$lambda_0($this);
}
function sendSignal($this, signal) {
  signal.room = $this.nh_1;
  // Inline function 'kotlin.js.asDynamic' call
  window.emitSignal(signal);
}
function handleRemoteData($this, data) {
  if (data.type == 'point') {
    // Inline function 'kotlin.js.asDynamic' call
    window.drawRemotePoint(data.x, data.y);
  }
}
function startListening($this) {
  // Inline function 'kotlin.js.asDynamic' call
  window.onRemoteSignal = WebRTCController$startListening$lambda($this);
}
function WebRTCController$slambda(this$0, resultContinuation) {
  this.fi_1 = this$0;
  CoroutineImpl.call(this, resultContinuation);
}
protoOf(WebRTCController$slambda).hi = function ($this$launch, $completion) {
  var tmp = this.ii($this$launch, $completion);
  tmp.b4_1 = Unit_instance;
  tmp.c4_1 = null;
  return tmp.h4();
};
protoOf(WebRTCController$slambda).r4 = function (p1, $completion) {
  return this.hi((!(p1 == null) ? isInterface(p1, CoroutineScope) : false) ? p1 : THROW_CCE(), $completion);
};
protoOf(WebRTCController$slambda).h4 = function () {
  var suspendResult = this.b4_1;
  $sm: do
    try {
      var tmp = this.z3_1;
      switch (tmp) {
        case 0:
          this.a4_1 = 2;
          this.z3_1 = 1;
          suspendResult = setupP2P(this.fi_1, this);
          if (suspendResult === get_COROUTINE_SUSPENDED()) {
            return suspendResult;
          }

          continue $sm;
        case 1:
          return Unit_instance;
        case 2:
          throw this.c4_1;
      }
    } catch ($p) {
      var e = $p;
      if (this.a4_1 === 2) {
        throw e;
      } else {
        this.z3_1 = this.a4_1;
        this.c4_1 = e;
      }
    }
   while (true);
};
protoOf(WebRTCController$slambda).ii = function ($this$launch, completion) {
  var i = new WebRTCController$slambda(this.fi_1, completion);
  i.gi_1 = $this$launch;
  return i;
};
function WebRTCController$slambda_0(this$0, resultContinuation) {
  var i = new WebRTCController$slambda(this$0, resultContinuation);
  var l = function ($this$launch, $completion) {
    return i.hi($this$launch, $completion);
  };
  l.$arity = 1;
  return l;
}
function WebRTCController$setupP2P$lambda(this$0) {
  return function (event) {
    var tmp;
    if (event.candidate != null) {
      sendSignal(this$0, {type: 'candidate', candidate: event.candidate});
      tmp = Unit_instance;
    }
    return Unit_instance;
  };
}
function WebRTCController$setupP2P$lambda_0(this$0) {
  return function (event) {
    setDataChannel(this$0, event.channel);
    return Unit_instance;
  };
}
function WebRTCController$setDataChannel$lambda() {
  println('DataChannel opened');
  return Unit_instance;
}
function WebRTCController$setDataChannel$lambda_0(this$0) {
  return function (event) {
    var tmp = JSON;
    var tmp_0 = event.data;
    var data = tmp.parse((!(tmp_0 == null) ? typeof tmp_0 === 'string' : false) ? tmp_0 : THROW_CCE());
    handleRemoteData(this$0, data);
    return Unit_instance;
  };
}
function WebRTCController$connectP2P$lambda$lambda(this$0, $offer) {
  return function () {
    sendSignal(this$0, $offer);
    return Unit_instance;
  };
}
function WebRTCController$connectP2P$lambda(this$0) {
  return function (offer) {
    return this$0.lh_1.setLocalDescription(offer).then(WebRTCController$connectP2P$lambda$lambda(this$0, offer));
  };
}
function WebRTCController$startListening$lambda$slambda($data, this$0, resultContinuation) {
  this.ri_1 = $data;
  this.si_1 = this$0;
  CoroutineImpl.call(this, resultContinuation);
}
protoOf(WebRTCController$startListening$lambda$slambda).hi = function ($this$launch, $completion) {
  var tmp = this.ii($this$launch, $completion);
  tmp.b4_1 = Unit_instance;
  tmp.c4_1 = null;
  return tmp.h4();
};
protoOf(WebRTCController$startListening$lambda$slambda).r4 = function (p1, $completion) {
  return this.hi((!(p1 == null) ? isInterface(p1, CoroutineScope) : false) ? p1 : THROW_CCE(), $completion);
};
protoOf(WebRTCController$startListening$lambda$slambda).h4 = function () {
  var suspendResult = this.b4_1;
  $sm: do
    try {
      var tmp = this.z3_1;
      if (tmp === 0) {
        this.a4_1 = 1;
        var tmp0_subject = this.ri_1.type;
        if (tmp0_subject == 'offer') {
          this.si_1.lh_1.setRemoteDescription(new RTCSessionDescription(this.ri_1)).then(WebRTCController$startListening$lambda$slambda$lambda(this.si_1));
        } else if (tmp0_subject == 'answer') {
          this.si_1.lh_1.setRemoteDescription(new RTCSessionDescription(this.ri_1));
        } else if (tmp0_subject == 'candidate') {
          this.si_1.lh_1.addIceCandidate(new RTCIceCandidate(this.ri_1.candidate));
        } else {
          handleRemoteData(this.si_1, this.ri_1);
        }
        return Unit_instance;
      } else if (tmp === 1) {
        throw this.c4_1;
      }
    } catch ($p) {
      var e = $p;
      throw e;
    }
   while (true);
};
protoOf(WebRTCController$startListening$lambda$slambda).ii = function ($this$launch, completion) {
  var i = new WebRTCController$startListening$lambda$slambda(this.ri_1, this.si_1, completion);
  i.ti_1 = $this$launch;
  return i;
};
function WebRTCController$startListening$lambda$slambda_0($data, this$0, resultContinuation) {
  var i = new WebRTCController$startListening$lambda$slambda($data, this$0, resultContinuation);
  var l = function ($this$launch, $completion) {
    return i.hi($this$launch, $completion);
  };
  l.$arity = 1;
  return l;
}
function WebRTCController$startListening$lambda(this$0) {
  return function (data) {
    return launch(this$0.kh_1, VOID, VOID, WebRTCController$startListening$lambda$slambda_0(data, this$0, null));
  };
}
function $setupP2PCOROUTINE$(_this__u8e3s4, resultContinuation) {
  CoroutineImpl.call(this, resultContinuation);
  this.wh_1 = _this__u8e3s4;
}
protoOf($setupP2PCOROUTINE$).h4 = function () {
  var suspendResult = this.b4_1;
  $sm: do
    try {
      var tmp = this.z3_1;
      switch (tmp) {
        case 0:
          this.a4_1 = 4;
          this.a4_1 = 3;
          this.z3_1 = 1;
          suspendResult = await_0(window.fetch('/ice-servers'), this);
          if (suspendResult === get_COROUTINE_SUSPENDED()) {
            return suspendResult;
          }

          continue $sm;
        case 1:
          var response = suspendResult;
          this.z3_1 = 2;
          suspendResult = await_0(response.json(), this);
          if (suspendResult === get_COROUTINE_SUSPENDED()) {
            return suspendResult;
          }

          continue $sm;
        case 2:
          var iceServers = suspendResult;
          var config = {};
          config.iceServers = iceServers;
          this.wh_1.lh_1 = new RTCPeerConnection(config);
          this.wh_1.lh_1.onicecandidate = WebRTCController$setupP2P$lambda(this.wh_1);
          this.wh_1.lh_1.ondatachannel = WebRTCController$setupP2P$lambda_0(this.wh_1);
          this.a4_1 = 4;
          this.z3_1 = 5;
          continue $sm;
        case 3:
          this.a4_1 = 4;
          var tmp_0 = this.c4_1;
          if (tmp_0 instanceof Exception) {
            var e = this.c4_1;
            println('Failed to setup P2P: ' + e.message);
            this.z3_1 = 5;
            continue $sm;
          } else {
            throw this.c4_1;
          }

        case 4:
          throw this.c4_1;
        case 5:
          this.a4_1 = 4;
          return Unit_instance;
      }
    } catch ($p) {
      var e_0 = $p;
      if (this.a4_1 === 4) {
        throw e_0;
      } else {
        this.z3_1 = this.a4_1;
        this.c4_1 = e_0;
      }
    }
   while (true);
};
function WebRTCController(signalingUrl) {
  this.signalingUrl = signalingUrl;
  this.kh_1 = MainScope();
  this.lh_1 = null;
  this.mh_1 = null;
  this.nh_1 = 'demo-room';
  startListening(this);
  launch(this.kh_1, VOID, VOID, WebRTCController$slambda_0(this, null));
}
protoOf(WebRTCController).ui = function () {
  return this.signalingUrl;
};
protoOf(WebRTCController).connectP2P = function () {
  if (this.lh_1 == null)
    return Unit_instance;
  this.mh_1 = this.lh_1.createDataChannel('chaos-sync');
  setDataChannel(this, this.mh_1);
  this.lh_1.createOffer().then(WebRTCController$connectP2P$lambda(this));
};
protoOf(WebRTCController).sendPoint = function (x, y) {
  var data = {type: 'point', x: x, y: y};
  if (this.mh_1 != null && this.mh_1.readyState == 'open') {
    this.mh_1.send(JSON.stringify(data));
  } else {
    // Inline function 'kotlin.js.asDynamic' call
    window.emitSignal(data);
  }
};
function main() {
  var controller = new WebRTCController(window.location.origin);
  // Inline function 'kotlin.js.asDynamic' call
  window.webrtcController = controller;
}
function mainWrapper() {
  main();
}
//region block: post-declaration
protoOf(InternalHashMap).o2 = containsAllEntries;
protoOf(CombinedContext).i7 = plus;
protoOf(AbstractCoroutineContextElement).m4 = get;
protoOf(AbstractCoroutineContextElement).h7 = fold;
protoOf(AbstractCoroutineContextElement).g7 = minusKey;
protoOf(AbstractCoroutineContextElement).i7 = plus;
protoOf(JobSupport).b9 = invokeOnCompletion$default;
protoOf(JobSupport).i7 = plus;
protoOf(JobSupport).m4 = get;
protoOf(JobSupport).h7 = fold;
protoOf(JobSupport).g7 = minusKey;
protoOf(CoroutineDispatcher).m4 = get_0;
protoOf(CoroutineDispatcher).g7 = minusKey_0;
//endregion
//region block: init
Companion_instance = new Companion();
Unit_instance = new Unit();
Companion_instance_2 = new Companion_2();
CompletedContinuation_instance = new CompletedContinuation();
Companion_instance_3 = new Companion_3();
Companion_instance_4 = new Companion_4();
Key_instance = new Key();
Companion_instance_6 = new Companion_6();
Active_instance = new Active();
Key_instance_1 = new Key_1();
Key_instance_2 = new Key_2();
NonDisposableHandle_instance = new NonDisposableHandle();
Key_instance_3 = new Key_3();
counter = 0;
DEBUG = false;
TaskContext_instance = new TaskContext();
//endregion
//region block: exports
export {
  WebRTCController as WebRTCController,
};
//endregion
mainWrapper();
