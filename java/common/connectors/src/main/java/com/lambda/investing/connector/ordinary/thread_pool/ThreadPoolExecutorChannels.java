package com.lambda.investing.connector.ordinary.thread_pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class ThreadPoolExecutorChannels extends ThreadPoolExecutor {
	private final String name;//not used
	private final boolean useThrottling;
	private final ConcurrentHashMap<Object, Channel> channelsDict;


	protected final Logger logger = LogManager.getLogger(ThreadPoolExecutorChannels.class);

	/***
	 *
	 * ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("name-%d").build();
	 * 		ThreadPoolExecutorOrderByChannel ex = new ThreadPoolExecutorOrderByChannel(null, 2, 2, 60, TimeUnit.SECONDS
	 * 				, new LinkedBlockingQueue<Runnable>(), namedThreadFactory, false);
	 *
	 * 		No handle RejectedExecutionException
	 * @param name
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 * @param factory
	 * @param useThrottling if true is going to process last item of the queue per channel/if false is normal FIFO
	 */
	public ThreadPoolExecutorChannels(String name
			, int corePoolSize, int maximumPoolSize
			, long keepAliveTime, TimeUnit unit
			, BlockingQueue<Runnable> workQueue, ThreadFactory factory
			, boolean useThrottling) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, factory);
		this.name = name;
		this.channelsDict = new ConcurrentHashMap<>();
		this.useThrottling = useThrottling;

	}

	public void execute(Runnable task, String channelName) {
		Channel channel = this.channelsDict.computeIfAbsent(channelName, k -> new Channel());
		channel.addTask(task);
		this.execute(channel);
	}

	protected void onTaskEnd(long duration, long skippedTasks) {

	}


	class Channel implements Runnable {
		private final BlockingDeque<Runnable> tasks;
		private final BlockingDeque<Long> timestamps;
		private final AtomicBoolean activityFlag;
		private final Object lock;
		private long skippedTasks;

		public Channel() {
			this.tasks = new LinkedBlockingDeque<>();
			this.timestamps = new LinkedBlockingDeque<>();
			this.activityFlag = new AtomicBoolean(false);
			this.lock = new Object();
			this.skippedTasks = 0;
		}

		public void addTask(Runnable newTask) {
			synchronized (this.lock) {
				this.tasks.add(newTask);
				this.timestamps.add(System.currentTimeMillis());
			}
		}

		@Override
		public void run() {
			if (activityFlag.compareAndSet(false, true)) {
				if (!this.tasks.isEmpty()) {
					Long startTime;
					Runnable task;
					synchronized (this.lock) {
						startTime = useThrottling ? this.timestamps.pollLast() : this.timestamps.poll();
						task = useThrottling ? this.selectBufferedTask() : this.selectUnbufferedTask();
					}
					task.run();
					onTaskEnd(System.currentTimeMillis() - startTime, this.skippedTasks);
				}
				activityFlag.set(false);
			} else {
				if (!this.tasks.isEmpty()) {
					execute(this);
				}

			}
		}

		private Runnable selectBufferedTask() {
			Runnable task = this.tasks.pollLast();
			this.skippedTasks = this.tasks.size();
			this.tasks.clear();
			this.timestamps.clear();
			return task;
		}

		private Runnable selectUnbufferedTask() {
			Runnable task = this.tasks.poll();
			this.skippedTasks = 0;
			return task;
		}
	}

}



