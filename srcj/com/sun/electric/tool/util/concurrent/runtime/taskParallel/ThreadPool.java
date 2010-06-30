/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ThreadPool.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.util.concurrent.runtime.taskParallel;

import java.util.ArrayList;

import com.sun.electric.database.Environment;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.UniqueIDGenerator;
import com.sun.electric.tool.util.concurrent.debug.LoadBalancing;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadID;
import com.sun.electric.tool.util.concurrent.runtime.WorkerStrategy;

/**
 * 
 * Magic thread pool
 * 
 * @author Felix Schmidt
 * 
 */
public class ThreadPool {

	/**
	 * states of the thread pool. This is very similar to states of processes or
	 * tasks.
	 */
	public enum ThreadPoolState {
		New, Init, Started, Closed, Sleeps;
	}

	public enum ThreadPoolType {
		simplePool, synchronizedPool
	}

	private IStructure<PTask> taskPool = null;
	private int numOfThreads = 0;
	private ArrayList<Worker> workers = null;
	private ThreadPoolState state;
	private UniqueIDGenerator generator;
	private UserInterface userInterface;
	private boolean debug;
	private ThreadPoolType type;

	/**
	 * prevent from creating thread pools via constructor
	 * 
	 * @param taskPool
	 * @param numOfThreads
	 */
	private ThreadPool(IStructure<PTask> taskPool, int numOfThreads, boolean debug, ThreadPoolType type) {
		state = ThreadPoolState.New;
		this.taskPool = taskPool;
		this.numOfThreads = numOfThreads;
		this.generator = new UniqueIDGenerator(0);
		this.debug = debug;
		this.type = type;

		// reset thread id
		ThreadID.reset();

		workers = CollectionFactory.createArrayList();

		setUserInterface(Job.getUserInterface());

		for (int i = 0; i < numOfThreads; i++) {
			workers.add(new Worker(this));
		}
		state = ThreadPoolState.Init;
	}

	/**
	 * start the thread pool
	 */
	public void start() {
		if (state == ThreadPoolState.Init) {
			for (Worker worker : workers) {
				worker.start();
			}
		}
		state = ThreadPoolState.Started;
	}

	/**
	 * shutdown the thread pool
	 */
	public void shutdown() throws InterruptedException {
		for (Worker worker : workers) {
			worker.shutdown();
			worker.strategy.trigger();
		}

		this.join();
		state = ThreadPoolState.Closed;

		// print statistics in debug mode
		if (this.debug) {
			LoadBalancing.getInstance().printStatistics();
			LoadBalancing.getInstance().reset();
		}
	}

	/**
	 * wait for termination
	 * 
	 * @throws InterruptedException
	 */
	public void join() throws InterruptedException {
		for (Worker worker : workers) {
			worker.join();
		}
	}

	/**
	 * Set thread pool to state sleep. Constraint: current State = started
	 */
	public void sleep() {
		if (state == ThreadPoolState.Started) {
			for (Worker worker : workers) {
				worker.sleep();
			}
			this.state = ThreadPoolState.Sleeps;
		}
	}

	/**
	 * Wake up the thread pool. Constraint: current State = sleeps
	 */
	public void weakUp() {
		if (this.state == ThreadPoolState.Sleeps) {
			for (Worker worker : workers) {
				worker.weakUp();
			}
			this.state = ThreadPoolState.Started;
		}
	}

	/**
	 * trigger workers (used for the synchronization)
	 */
	public void trigger() {
		for (Worker worker : workers) {
			worker.strategy.trigger();
		}
	}

	/**
	 * add a task to the pool
	 * 
	 * @param item
	 */
	public void add(PTask item) {
		taskPool.add(item);
	}

	/**
	 * 
	 * @return the current thread pool size (#threads)
	 */
	public int getPoolSize() {
		return this.numOfThreads;
	}

	/**
	 * Worker class. This class uses a worker strategy to determine how to
	 * process tasks in the pool.
	 */
	protected class Worker extends Thread {

		private ThreadPool pool;
		private PoolWorkerStrategy strategy;

		public Worker(ThreadPool pool) {
			this.pool = pool;
			ThreadID.set(generator.getUniqueId());
			strategy = PoolWorkerStrategyFactory.createStrategy(taskPool, type);
			if (pool.debug) {
				LoadBalancing.getInstance().registerWorker(strategy);
			}
		}

		@Override
		public void run() {

			pool.taskPool.registerThread();

			try {
				Job.setUserInterface(pool.getUserInterface());
				Environment.setThreadEnvironment(Job.getUserInterface().getDatabase().getEnvironment());
			} catch (Exception ex) {

			}

			// execute worker strategy (all process of a worker is defined in a
			// strategy)
			strategy.execute();
		}

		/**
		 * shutdown the current worker
		 */
		public void shutdown() {
			strategy.shutdown();
		}

		/**
		 * Danger: Could cause deadlocks
		 */
		public void sleep() {
			strategy.pleaseWait();
		}

		/**
		 * Danger: Could cause deadlocks
		 */
		public void weakUp() {
			strategy.pleaseWakeUp();
			synchronized (strategy) {
				strategy.notifyAll();
			}
		}

	}

	/**
	 * Factory class for worker strategy
	 */
	private static class PoolWorkerStrategyFactory {
		public static PoolWorkerStrategy createStrategy(IStructure<PTask> taskPool, ThreadPoolType type) {
			if (type == ThreadPoolType.synchronizedPool)
				return new SynchronizedWorker(taskPool);
			else
				return new SimpleWorker(taskPool);
		}
	}

	private static ThreadPool instance = null;

	/**
	 * initialize thread pool, default initialization
	 * 
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize() throws PoolExistsException {
		return ThreadPool.initialize(false);
	}

	/**
	 * initialize thread pool, default initialization
	 * 
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(boolean debug) throws PoolExistsException {
		return ThreadPool.initialize(ThreadPool.getNumOfThreads(), debug);
	}

	/**
	 * initialize thread pool with number of threads
	 * 
	 * @param num
	 *            of threads
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(int num, boolean debug) throws PoolExistsException {
		IStructure<PTask> taskPool = CollectionFactory.createLockFreeQueue();
		return ThreadPool.initialize(taskPool, num, debug);
	}

	/**
	 * initialize thread pool with number of threads
	 * 
	 * @param num
	 *            of threads
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(int num) throws PoolExistsException {
		IStructure<PTask> taskPool = CollectionFactory.createLockFreeQueue();
		return ThreadPool.initialize(taskPool, num, false);
	}

	/**
	 * initialize thread pool with specific task pool
	 * 
	 * @param taskPool
	 *            to be used
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(IStructure<PTask> taskPool, boolean debug) throws PoolExistsException {
		return ThreadPool.initialize(taskPool, ThreadPool.getNumOfThreads(), debug);
	}

	/**
	 * initialize thread pool with specific task pool
	 * 
	 * @param taskPool
	 *            to be used
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(IStructure<PTask> taskPool) throws PoolExistsException {
		return ThreadPool.initialize(taskPool, false);
	}

	/**
	 * initialize thread pool with specific task pool and number of threads
	 * 
	 * @param taskPool
	 *            to be used
	 * @param numOfThreads
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static synchronized ThreadPool initialize(IStructure<PTask> taskPool, int numOfThreads)
			throws PoolExistsException {
		return ThreadPool.initialize(taskPool, numOfThreads, false);
	}

	/**
	 * initialize thread pool with specific task pool and number of threads
	 * 
	 * @param taskPool
	 * @param numOfThreads
	 * @param debug
	 * @return
	 * @throws PoolExistsException
	 */
	public static synchronized ThreadPool initialize(IStructure<PTask> taskPool, int numOfThreads,
			boolean debug) throws PoolExistsException {
		return initialize(taskPool, numOfThreads, debug, ThreadPoolType.simplePool);
	}

	/**
	 * initialize thread pool with specific task pool and number of threads
	 * 
	 * @param taskPool
	 *            to be used
	 * @param numOfThreads
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static synchronized ThreadPool initialize(IStructure<PTask> taskPool, int numOfThreads,
			boolean debug, ThreadPoolType type) throws PoolExistsException {
		if (ThreadPool.instance == null || instance.state != ThreadPoolState.Started) {
			instance = new ThreadPool(taskPool, numOfThreads, debug, type);
			instance.start();
		} else {
			return instance;
		}

		return instance;
	}

	/**
	 * create a double thread pool (two thread pool side by side)
	 * 
	 * @param taskPool1
	 * @param numOfThreads1
	 * @param type1
	 * @param taskPool2
	 * @param numOfThreads2
	 * @param type2
	 * @param debug
	 * @return
	 */
	public static synchronized ThreadPool[] initialize(IStructure<PTask> taskPool1, int numOfThreads1,
			ThreadPoolType type1, IStructure<PTask> taskPool2, int numOfThreads2, ThreadPoolType type2,
			boolean debug) {

		ThreadPool[] result = new ThreadPool[2];

		result[0] = new ThreadPool(taskPool1, numOfThreads1, debug, type1);
		result[1] = new ThreadPool(taskPool2, numOfThreads2, debug, type2);

		result[0].start();
		result[1].start();

		return result;

	}

	/**
	 * hard shutdown of thread pool
	 */
	public static synchronized void killPool() {
		try {
			ThreadPool.instance.shutdown();
		} catch (InterruptedException e) {}
		ThreadPool.instance = null;
	}

	private static int getNumOfThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	/**
	 * returns the current thread pool
	 * 
	 * @return thread pool
	 */
	public static ThreadPool getThreadPool() {
		return instance;
	}

	/**
	 * set the user interface for the thread pool
	 * 
	 * @param userInterface
	 */
	public void setUserInterface(UserInterface userInterface) {
		this.userInterface = userInterface;
	}

	/**
	 * get the user interface of the thread pool
	 * 
	 * @return
	 */
	public UserInterface getUserInterface() {
		return userInterface;
	}

	/**
	 * Get current state of the thread pool
	 * 
	 * @return
	 */
	public ThreadPoolState getState() {
		return state;
	}

	/**
	 * Get true if the current thread pool runs in debug mode, otherwise false
	 * 
	 * @return
	 */
	public boolean getDebug() {
		return this.debug;
	}
}