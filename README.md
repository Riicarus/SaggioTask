# SaggioTask

## 概述

SaggioTask 是一个基于 Java 的复杂任务编排组件, 提供了便捷的任务编排 API 以及完善的任务生命周期, 并对任务处理进行了优化.

和一些任务编排组件不同, SaggioTask 使用了下推表作为任务编排的核心数据结构, 可以根据一个任务执行返回的不同状态执行不同的后续任务.

## 快速入手

### Maven 导入

```pom
<dependency>
    <groupId>io.github.riicarus</groupId>
    <artifactId>SaggioTask</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 实例定义

要使用 SaggioTask API, 需要先创建一个 `SaggioTask` 实例, 之后一系列的任务编排都是在该实例的上下文中完成的.

```java
SaggioTask saggioTask = new SaggioTask();
```

### 任务创建

本组件的核心单元就是任务, 需要先创建出任务再对其进行编排. 任务的核心接口是 `TransferableTask`, 我们基于它, 使用 `SaggioTask` 类提供的 API 进行任务创建.

`SaggioTask` 类提供了一些用于创建任务的 API, 如下:

```java
public <T> TransferableTask<T> build(String name, TaskFunction<T> taskFunc);

public <T> TransferableTask<T> build(String name, TaskFunction<T> taskFunc, TaskCallback<T> callback);

public <T> TransferableTask<T> build(String name, PrevTaskFunction prevFunc, TaskFunction<T> taskFunc, TaskCallback<T> callback)

public <T> TransferableTask<T> buildFrom(String name, TransferableTask<T> srcTask);
```

构建一个任务时, 任务名称 `name` 和 任务函数 `taskFunc` 是必要的. 我们也提供了任务生命周期中在任务执行前后执行的函数 `prevFunc` 和 `callback` 函数, 这些是非必要的, 用于强化功能.

当然, 如果有相同逻辑的任务, 但是处在任务编排中的不同位置, 可以使用 `SaggioTask#buildFrom()` 方法来快捷地进行任务的复制, 当然, 这只会复制该任务生命周期中的三个执行函数 `prevFunc`, `taskFunc` 和 `callback`.

上述三个任务周期中的执行函数都是接口, 因此支持使用 lambda 表达式进行快捷声明.

```java
TransferableTask<String> taskB = saggioTask.build("B", (ctx) -> {
    Thread.sleep(4000);
    return new TaskResult<>("success", "B-D1");
}, (res, ctx) -> System.out.println(res));
```

需要注意, `TransferableTask<T>` 中的泛型 `T` 和 `TaskFunction<T>` 以及 `TaskCallback<T>` 的泛型类型一致, 表示任务函数 `TaskFunction` 的返回类 `TaskResult<T>` 中的泛型.

以下是三个接口的定义:

```java
public interface PrevTaskFunction {
    void execute(TaskContext context);
}

public interface TaskFunction<T> {
    TaskResult<T> execute(TaskContext context) throws InterruptedException;
}

public interface TaskCallback<T> {
    void execute(TaskResult<T> result, TaskContext context);
}
```

`TaskResult` 是 `TaskFunction` 的执行结果, 包含了 `data` 和 `state` 属性, 用于回调和状态转移.

```java
public class TaskResult<T> {
    private T t;
    private String state;
}
```

### 任务编排

`TransferableTask` 提供了两个用于编排任务的 API, `TransferableTask#and()` 和 `TransferableTask#any()`.

`taskA.and(taskB, state)` 表示需要 `taskB` 返回的状态为 `state` 时, 才会执行 `taskA`, `and()` 的语义在于必须 `taskA` 必须等待所有通过 `taskA.and()` 编排的任务返回正确的状态时, 才能够执行.

同理, `taskA.any(taskB, state)` 语义在于 `taskA` 只需要等待任何一个通过 `taskA.any()` 编排的任务返回正确的状态, 就可以执行了.

> 注意: `and()` 和 `any()` 方法不能同时使用, 否则会报错:
> "Type is already set to 'ANY'/'AND', can not add task of type 'AND'/'ANY', task: ..."

下面是一个编排任务的例子:

```java
TransferableTask<String> task0 = saggioTask.build("0",
        (ctx) -> new TaskResult<>("success", "0"),
        (res, ctx) -> System.out.println(res));
TransferableTask<String> taskA = saggioTask.build("A",
        (ctx) -> new TaskResult<>("success", "A-D"),
        (res, ctx) -> System.out.println(res));
TransferableTask<String> taskB = saggioTask.build("B",
        (ctx) -> {
            Thread.sleep(4000);
            return new TaskResult<>("success", "B-D");
        },
        (res, ctx) -> System.out.println(res));
TransferableTask<String> taskC = saggioTask.build("C",
        (ctx) -> {
            Thread.sleep(4000);
            return new TaskResult<>("success", "C-D");
        },
        (res, ctx) -> System.out.println(res));
TransferableTask<String> taskD = saggioTask.build("D",
        (ctx) -> new TaskResult<>("success", "D-E"),
        (res, ctx) -> System.out.println(res));

taskA.and(task0, "0");
taskB.and(task0, "0");
taskC.and(task0, "0");
taskD.any(taskA, "A-D")
        .any(taskB, "B-D")
        .any(taskC, "C-D");
```

执行顺序为 `task0("0") -> taskA & taskB & taskC`, `taskA("A-D") | taskB("B-D") | taskC("C-D") -> taskD`.

### 任务执行

之前的工作只是对未来需要执行的任务进行了编排, 并没有执行, 我们需要使用 `SaggioTask#run()` 来执行这一系列任务.

```java
public void run(List<TransferableTask<?>> tasks, ThreadPoolExecutor executor, TaskContext context);
```

`SaggioTask#run()` 支持同时开始执行一系列任务, 支持使用自定义的线程池作为执行线程池, `TaskContext` 为任务执行过程中的上下文, 可以在其中进行一些数据的缓存操作.

对于上面任务编排的例子, 任务执行的示例为:

```java
ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(20, 50, 100, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
TaskContext context = new TaskContext();

List<TransferableTask<?>> startTasks = new ArrayList<>();
startTasks.add(task0);

saggioTask.run(startTasks, taskExecutor, context);
```

## 设置与优化

### 超时

`SaggioTask` 中, 每个任务都可以设置超时时间, 相关属性为 `timeout` 和 `timeUnit`, 默认为 `3000 ms`. 可以通过 `TransferableTask#setTimeout()` 来设置, 如果手动设置了单个 task 中的超时属性, `useCustomizedTimeout` 会被置为 `true`, 表示使用当前设置的超时时间, 而不是全局的.

```java
public TransferableTask<?> setTimeout(int timeout, TimeUnit timeUnit);
```

全局超时时间通过 `TaskConfig#setTimeout()` 进行设置, 被 `TaskContext` 封装:

```java
TaskContext context = new TaskContext();
context.getConfig().setTimeout(1000, TimeUnit.MILLISECONDS);
```

全局超时时间使用优先级低于用户自定义的单个任务超时时间, 仅对用户没有主动设置超时时间的任务生效.

全局超时时间的默认值同样为 `3000 ms`.

### 任务终止

对于一些任务编排场景来说, 如果按编排方法: `taskA("A-D") & taskB("B-D") & taskC("C-D") -> taskD`:

如果 `taskA` 执行失败, 为了执行效率, 需要同时终止 `taskB` 和 `taskC`, 此时可以使用 `TaskConfig#setRecursivelyStop(true)` 方法来使任务在执行过程中, 如果后续任务无法执行, 自动停止其前面的所有任务.

该设置同样对以下情况生效:

- 在 `ANY` 逻辑中, 前置任务任意一个生效, 停止其余所有前置任务.
- 任何逻辑中, 任务等待超时, 停止其后续任务以及相关的前置任务.

使用方法:

```java
TaskContext context = new TaskContext();
context.getConfig().setRecursivelyStop(true);
```

反之, 如果将其设置为 `false`, 则相关任务的前置任务无论如何都需要执行结束, 不会被提前终止.

### 快捷编排

上面使用纯代码对 `Task` 对象进行编排的过程中很麻烦, 我们提供了相对快捷的编排方式: 使用一种简单的 `dsl` 进行编排.

如: 对于编排方法: `taskA("A-D") & taskB("B-D") & taskC("C-D") -> taskD`:

使用内置的 dsl 表述为: `taskA#A-D, taskB#B-D, taskC#C-D & taskD @3000`.

使用 `SaggioTask#arrange()` 方法对任务进行快捷编排, 使用任务的名称对任务进行指代, 需要任务预先被注册到 `SaggioTask` 中(在 `SaggioTask#build()` 方法中自动完成).

```java
saggioTask.arrange("task#0, task0#aa & taskA, taskB, taskC @1000");
```

#### DSL 简易语法

由于是一个很简单的语法, 我们遵从一个固定的格式:

`taskName#state[, taskName@state...] LINKER taskName[, taskName...] @timeout`

- `LINKER` 表示 task 之间的关系, 是 `AND` 还是 `ANY`, 对应值为: `&` 或 `|`.
- `LINKER` 之前的部分我们称之为 `FromTaskEntry`, 包含两个部分:
  - `taskName` 是 task 在 `SaggioTask` 中注册的名称.
  - `state` 是 task 返回的 `state` 值.
  - `taskName` 和 `state` 之间使用 `#` 隔开, 不能有空格.
- `LINKER` 之后的部分我们称之为 `ToTask`, 表示 `FromTaskEntry` 执行成功后需要执行的下一步的 task(s).
- `@timeout` 用于指定当前编排任务的过期时间, 单位为 `ms`, 注意不能有空格.
- 除特殊指定不能有空格的结构外, 其余结构使用一个空格隔开.