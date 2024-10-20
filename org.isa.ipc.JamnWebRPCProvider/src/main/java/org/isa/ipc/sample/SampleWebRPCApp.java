package org.isa.ipc.sample;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebRPCProvider;
import org.isa.ipc.web.api.About;

public class SampleWebRPCApp {

	public static void main(String[] args) throws Exception{
		//create a server
		JamnServer lServer = new JamnServer();
		lServer.getConfig().setCORSEnabled(true); // required for localhost communication via js fetch

		//create the RPC provider
		JamnWebRPCProvider lWebRPCProvider = new JamnWebRPCProvider(lServer.getLoggerFor(JamnWebRPCProvider.class.getName()));
		//register rpc api services
		lWebRPCProvider.registerApiService(About.class);
		
		//get the actual provider and add it to the server
		lServer.addContentProvider("RPCProvider", lWebRPCProvider.getJamnContentProvider());
		//start server
		lServer.start();
	}
}
