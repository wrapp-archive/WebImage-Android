package com.wrapp.android.webimage;

import java.util.concurrent.ThreadFactory;

class PriorityThreadFactory implements ThreadFactory {
  private int priority;

  public PriorityThreadFactory(int priority) {
    this.priority = priority;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(r);
    t.setPriority(priority);

    return t;
  }
}