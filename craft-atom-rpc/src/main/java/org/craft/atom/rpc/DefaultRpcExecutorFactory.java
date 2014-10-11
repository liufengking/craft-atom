package org.craft.atom.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.craft.atom.protocol.rpc.model.RpcMessage;
import org.craft.atom.protocol.rpc.model.RpcMethod;
import org.craft.atom.rpc.api.RpcParameter;
import org.craft.atom.rpc.spi.RpcEntry;
import org.craft.atom.rpc.spi.RpcExecutorFactory;
import org.craft.atom.rpc.spi.RpcRegistry;
import org.craft.atom.util.NamedThreadFactory;

/**
 * @author mindwind
 * @version 1.0, Aug 12, 2014
 */
public class DefaultRpcExecutorFactory implements RpcExecutorFactory {

	
	@Getter @Setter private RpcRegistry                  registry;
	@Getter @Setter private Map<String, ExecutorService> pool    ;
	
	
	// ~ ------------------------------------------------------------------------------------------------------------
	
	
	public DefaultRpcExecutorFactory() {
		this.pool = new ConcurrentHashMap<String, ExecutorService>();
	}
	
	
	// ~ ------------------------------------------------------------------------------------------------------------
	
	
	@Override
	public ExecutorService getExecutor(RpcMessage msg) {
		String          rpcId        = msg.getBody().getRpcId(); 
		RpcMethod       rpcMethod    = msg.getBody().getRpcMethod();
		Class<?>        rpcInterface = msg.getBody().getRpcInterface();
		DefaultRpcEntry entry        = new DefaultRpcEntry(rpcId, rpcInterface, rpcMethod);
		return getExecutor(entry);
	}
	
	private ExecutorService getExecutor(RpcEntry queryEntry) {
		String key = queryEntry.getKey();
		ExecutorService es = pool.get(key);
		if (es == null) {
			synchronized (this) {
				if (es == null) {
					RpcEntry     entry     = registry.lookup(queryEntry);
					RpcParameter parameter = entry.getRpcParameter();
					int          threads   = parameter.getRpcThreads() == 0 ? 1 : parameter.getRpcThreads();
					int          queues    = parameter.getRpcQueues()  == 0 ? 1 : parameter.getRpcQueues() ;
					ThreadPoolExecutor tpe = new RpcThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(queues), new NamedThreadFactory("craft-atom-rpc"));
					tpe.allowCoreThreadTimeOut(true);
					es = tpe;
					pool.put(key, tpe);
				}
			}
		}
		return es;
	}

}