/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.storage.StorageThread;

public abstract class Stage extends LifeCycleThread {
    
    /**
     * queue containing all requests
     */
    protected BlockingQueue<StageRequest> q;
    
    private final int queueCapacity;

    private boolean                       printOut = false;

    /**
     * set to true if stage should shut down
     */
    protected volatile boolean            quit;
    
    public AtomicInteger                  _numRq, _maxRqTime, _minRqTime;
    
    public AtomicLong                     _sumRqTime;

    private StageRequest                  op;
    
    public Stage(String stageName, int queueCapacity) {
        
        super(stageName);
        q = new LinkedBlockingQueue<StageRequest>();
        this.queueCapacity = queueCapacity;
        this.quit = false;
        
        _numRq = new AtomicInteger(0);
        _maxRqTime = new AtomicInteger(0);
        _minRqTime = new AtomicInteger(Integer.MAX_VALUE);
        _sumRqTime = new AtomicLong(0);
    }
    
    /**
     * send an request for a stage operation
     * 
     * @param stageOp
     * @param args
     * @param request
     * @param callback
     */
    protected void enqueueOperation(int stageOp, Object[] args, OSDRequest request, Object callback) {
        enqueueOperation(stageOp, args, request, null, callback);
    }
    
    /**
     * 
     * send an request for a stage operation
     * 
     * @param stageOp
     *            stage op number
     * @param args
     *            arguments
     * @param request
     *            request
     * @param callback
     *            callback
     * @param createdViewBuffer
     *            an optional additional view buffer to the data, which will be
     *            freed if the request needs to be dropped due to overload
     */
    protected void enqueueOperation(int stageOp, Object[] args, OSDRequest request, ReusableBuffer createdViewBuffer,
            Object callback) {
        // rq.setEnqueueNanos(System.nanoTime());
        String logMessage = "";
        int queueSize = q.size();
        try {
            if (stageOp == StorageThread.STAGEOP_GET_GMAX) {
                
                if(!printOut){
                    logMessage += "\n--- Stage - enqueue - Stacktrace:";
                    Thread.currentThread().getStackTrace().toString();
                    printOut = true;
                }
                
                logMessage += "\n --- STAGEOP_GET_GMAX enqueue on " + getName();

                if (request != null && request.getRpcRequest() != null && request.getRpcRequest().getHeader() != null) {
                    logMessage += "\n --- call_id: " + request.getRpcRequest().getHeader().getCallId() + " #";
                } else {
                    logMessage += "\n --- RpcReq / Header null";
                }

                logMessage += "\n --- Queue-Size (before): " + queueSize;
                if (queueSize > 5) {
                    logMessage += "\n --- Queue Size > 5, listing Queue: ";
                    if (op != null) {
                        logMessage += "\n --- Current Op: " + op.getStageMethod();
                        if (op.getRequest() != null && op.getRequest().getRpcRequest() != null) {
                            logMessage += "\n" + op.getRequest().getRpcRequest().toString();
                        } else {
                            logMessage += "\nCouldn't get RpcRequest";
                        }
                    }
                    StageRequest req = q.peek();
                    if (req != null) {
                        logMessage += "\n --- Next Req: " + req.getStageMethod();
                        if (req.getRequest() != null && req.getRequest().getRpcRequest() != null) {
                            logMessage += "\n" + req.getRequest().getRpcRequest().toString();
                        } else {
                            logMessage += "\nCouldn't get RpcRequest";
                        }
                    }
                }
            }
        } catch (Exception e) {
            logMessage += "\n--- Exception catch: " + e.getMessage();
        } finally {
            if (!logMessage.isEmpty()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.all, this, logMessage);
            }
        }

        if (request == null) {
            try {
                q.put(new StageRequest(stageOp, args, request, callback));
            } catch (InterruptedException e) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.stage, this,
                        "Failed to queue internal request due to InterruptedException:");
                Logging.logError(Logging.LEVEL_INFO, this, e);
            }
        } else {
            if (q.size() < queueCapacity) {
                try {
                    q.put(new StageRequest(stageOp, args, request, callback));
                } catch (InterruptedException e) {
                    Logging.logMessage(Logging.LEVEL_INFO, Category.stage, this,
                            "Failed to queue external request due to InterruptedException:");
                    Logging.logError(Logging.LEVEL_INFO, this, e);
                }
            } else {
                // Make sure that the data buffer is returned to the pool if
                // necessary, as some operations create view buffers on the
                // data. Otherwise, a 'finalized but not freed before' warning
                // may occur.
                if (createdViewBuffer != null) {
                    assert (createdViewBuffer.getRefCount() >= 2);
                    BufferPool.free(createdViewBuffer);
                }
                Logging.logMessage(Logging.LEVEL_WARN, this, "stage is overloaded, request %d for %s dropped",
                        request.getRequestId(), request.getFileId());
                request.sendInternalServerError(new IllegalStateException("server overloaded, request dropped"));
            }
        }

        if (stageOp == StorageThread.STAGEOP_GET_GMAX && q.size() != (queueSize + 1)) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.all, this, " --- Queue-Size (after): " + q.size());
        }
    }
    
    /**
     * shut the stage thread down
     */
    @Override
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }
    
    /**
     * Get current number of requests in the queue.
     * 
     * @return queue length
     */
    public int getQueueLength() {
        return q.size();
    }
    
    @Override
    public void run() {
        
        notifyStarted();
        
        while (!quit) {
            try {
                if (getName().startsWith("OSD StThr") && q.size() > 10) {
                    Logging.logMessage(Logging.LEVEL_INFO, Category.all, this, getName() + " queue size: " + q.size());
                }

                op = q.take();

                processMethod(op);
                
            } catch (InterruptedException ex) {
                break;
            } catch (Throwable ex) {
                this.notifyCrashed(ex);
                break;
            }
        }
        
        notifyStopped();
    }
    
    protected void calcRequestDuration(OSDRequest rq) {
        /*
         * long d = (System.nanoTime()-rq.getEnqueueNanos())/100000l;
         * _numRq.incrementAndGet(); if (_minRqTime.get() > d)
         * _minRqTime.set((int)d); if (_maxRqTime.get() < d)
         * _maxRqTime.set((int)d); _sumRqTime.addAndGet(d);
         */
    }
    
    /**
     * Handles the actual execution of a stage method. Must be implemented by
     * all stages.
     * 
     * @param method
     *            the stage method to execute
     */
    protected abstract void processMethod(StageRequest method);
    
    public static interface NullCallback {
        public void callback(ErrorResponse ex);
    }
    
    public static final class StageRequest {
        
        private final int              stageMethod;
        
        private final Object           callback;
        
        private final Object[]         args;
        
        private final OSDRequest request;
        
        public StageRequest(int stageMethod, Object[] args, OSDRequest request, Object callback) {
            this.args = args;
            this.stageMethod = stageMethod;
            this.callback = callback;
            this.request = request;
        }
        
        public int getStageMethod() {
            return stageMethod;
        }
        
        public Object[] getArgs() {
            return args;
        }
        
        public Object getCallback() {
            return callback;
        }
        
        public OSDRequest getRequest() {
            return request;
        }
        
        public void sendInternalServerError(Throwable cause) {
            if (request != null) {
                request.sendInternalServerError(cause);
            } else {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "internal server error in internal event: %s",
                    cause.toString());
                Logging.logError(Logging.LEVEL_ERROR, this, cause);
            }
        }
    }
    
}
