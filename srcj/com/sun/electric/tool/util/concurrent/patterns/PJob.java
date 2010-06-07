/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PJob.java
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
package com.sun.electric.tool.util.concurrent.patterns;

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;

/**
 * Parallel job. This job ends if a new further tasks for this job are
 * available.
 * 
 * @author Felix Schmidt
 * 
 */
public class PJob {

	public static final int SERIAL = -1;

	protected AtomicInteger numOfTasksTotal = new AtomicInteger(0);
	protected AtomicInteger numOfTasksFinished = new AtomicInteger(0);
	protected ThreadPool pool;

	public PJob() {
		this.pool = ThreadPool.getThreadPool();
	}

	/**
	 * Call back function for thread pool workers, to tell this job that a task
	 * of this job is finished.
	 */
	public synchronized void finishTask() {
		numOfTasksFinished.incrementAndGet();
	}

	/**
	 * Executor method of a job. This function uses the default and block after
	 * starting the job.
	 */
	public void execute() {
		this.execute(true);
	}

	/**
	 * Executor method of a job. In some cases it could be necessary to start
	 * the job non-blocking, e.g. for parallel jobs.
	 */
	public void execute(boolean block) {

		// pool.start();

		if (block) {
			this.join();
		}
	}

	/**
	 * Wait for the job while not finishing.
	 */
	public void join() {
		while (numOfTasksFinished.get() != numOfTasksTotal.get()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Use this method to add tasks to this job. This will affect that the new
	 * task is registered by this job.
	 * 
	 * @param task
	 */
	public void add(PTask task, int threadID) {
		numOfTasksTotal.incrementAndGet();
		pool.add(task);
	}

	/**
	 * Use this method to add tasks to this job. This will affect that the new
	 * task is registered by this job.
	 * 
	 * @param task
	 */
	public void add(PTask task) {
		this.add(task, SERIAL);
	}

}
