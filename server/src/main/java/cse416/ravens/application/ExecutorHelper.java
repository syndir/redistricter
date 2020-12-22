package cse416.ravens.application;

import java.util.concurrent.*;

public class ExecutorHelper {
    private ThreadPoolExecutor executor = null;
    private static ExecutorThreadFactory threadFactory = new ExecutorThreadFactory();
    private static ExecutorHelper executorHelperInstance = null;
    private static Integer maxServerProcessingThreads = 12;

    private ExecutorHelper() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxServerProcessingThreads);
        executor.setThreadFactory(threadFactory);
    }

    public static ExecutorHelper getInstance() {
        if (executorHelperInstance == null) {
            ExecutorHelper.executorHelperInstance = new ExecutorHelper();
        }
        return ExecutorHelper.executorHelperInstance;
    }

    public static void setMaxServerProcessingThreads(Integer maxServerThreads) {
        ExecutorHelper.maxServerProcessingThreads = maxServerThreads;
    }

	public Future<?> submit(Callable<?> task) {
        DebugHelper.log("Executor#submit() (task=" + task.toString() + ")");
        return executor.submit(task);
    }
    
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }
}

class ExecutorThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    }
}