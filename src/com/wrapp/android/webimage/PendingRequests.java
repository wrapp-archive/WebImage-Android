package com.wrapp.android.webimage;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Future;

class PendingRequests {
  private WeakHashMap<ImageRequest.Listener, ImageRequest> pendingListeners;
  private WeakHashMap<ImageRequest, PendingTask> pendingTasks;
  
  public PendingRequests() {
    pendingListeners = new WeakHashMap<ImageRequest.Listener, ImageRequest>();
    pendingTasks = new WeakHashMap<ImageRequest, PendingTask>();
  }
  
  public boolean isPending(ImageRequest request) {
    return pendingTasks.containsKey(request);
  }
  
/**
 * Add a request
 * 
 * @param request resource to fetch
 * @param listener who is interested in the result
 * @return true if possible without anything more, false if you need to add
   * a task via {@code addTask()}
 */
  public boolean addRequest(ImageRequest request, ImageRequest.Listener listener) {
    ImageRequest pendingRequest = pendingListeners.get(listener);
    if (request.equals(pendingRequest)) {
      // We are already pending for this request, do nothing
      return true;
    } else if (pendingRequest != null) {
      // This listener is pending for another request, remove us from it
      LogWrapper.logMessage("Already pending, old: " + pendingRequest.imageUrl + ", new: " + request.imageUrl);
      removeListener(listener);
    }
    
    pendingListeners.put(listener, request);
    
    PendingTask pendingTask = pendingTasks.get(request);
    if (pendingTask == null) {
      return false;
    } else {
      LogWrapper.logMessage("Reusing existing request: " + request.imageUrl);
      pendingTask.addListener(listener);
      
      return true;
    }
  }
  
  public void addTask(ImageRequest request, ImageRequest.Listener listener, Future<?> task) {
    PendingTask pendingTask = new PendingTask(task);
    pendingTasks.put(request, pendingTask);
    
    pendingTask.addListener(listener);
  }
  
  public void swapFuture(ImageRequest request, Future<?> future) {
    PendingTask task = pendingTasks.get(request);
    task.future = future;
  }
  
  public void removeListener(ImageRequest.Listener listener) {
    ImageRequest request = pendingListeners.remove(listener);
    
    if (!pendingTasks.get(request).removeListener(listener)) {
      // We were the only ones waiting for this task, remove it
      pendingTasks.remove(request);
    }
  }

  public void clear() {
    for (Map.Entry<ImageRequest, PendingTask> entry : pendingTasks.entrySet()) {
      PendingTask task = entry.getValue();
      task.future.cancel(false);
      
      // Notify that we were actually cancelled
      for (ImageRequest.Listener listener : task.getListeners()) {
        listener.onBitmapLoadCancelled();
      }
    }

    pendingTasks.clear();
    pendingListeners.clear();
  }

  public Set<ImageRequest.Listener> getListeners(ImageRequest request) {
    PendingTask task = pendingTasks.get(request);
    return task.getListeners();
  }
  
  public void removeRequest(ImageRequest request) {
    PendingTask task = pendingTasks.get(request);
    
    Set<ImageRequest.Listener> listeners = task.getListeners();
    pendingListeners.keySet().removeAll(listeners);
    
    pendingTasks.remove(request);
  }
  
  private static class PendingTask {
    public Future<?> future;
    
    // This is used as a Set but needs to be a Map because
    // Collections.newSetFromMap() was only introduced in API 9
    private Map<ImageRequest.Listener, Boolean> listeners;
    
    public PendingTask(Future<?> future) {
      this.future = future;
      
      listeners = new WeakHashMap<ImageRequest.Listener, Boolean>();
    }
    
    public void addListener(ImageRequest.Listener listener) {
      listeners.put(listener, Boolean.TRUE);
    }
    
    /**
     * Remove a listener
     * @return true if this task is still pending
     */
    public boolean removeListener(ImageRequest.Listener listener) {
      if (!listeners.remove(listener)) {
        throw new IllegalStateException("Request was not pending for this task");
      }
      
      if (listeners.isEmpty()) {
        // No other listeners, cancel this task
        // It doesn't matter if we successfully cancel it, if we don't
        // then it will be ignored in the callback anyways
        
        // We could interrupt the thread here to have it cancel a possible
        // download, however this is not really wanted as the image will probably
        // be needed at a later point, also there seems to be a possibility of a deadlock
        // for some reason at java.nio.channels.spi.AbstractInterruptibleChannel.close(AbstractInterruptibleChannel.java:~77)
        future.cancel(false);
        
        return false;
      } else {
        // Task is still pending for others
        return true;
      }
    }
    
    public Set<ImageRequest.Listener> getListeners() {
      return listeners.keySet();
    }
  }
}