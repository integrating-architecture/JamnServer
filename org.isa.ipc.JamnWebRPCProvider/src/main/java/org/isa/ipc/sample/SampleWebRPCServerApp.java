package org.isa.ipc.sample;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebRPCProvider;
import org.isa.ipc.JamnWebRPCProvider.JsonToolWrapper;
import org.isa.ipc.sample.web.api.SampleWebApiServices;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SampleWebRPCServerApp {

	// wrap a json tool for the JamnWebRPCProvider
	private static JsonToolWrapper Jackson = new JamnWebRPCProvider.JsonToolWrapper() {
		private final ObjectMapper Jack = new ObjectMapper().setVisibility(PropertyAccessor.FIELD,
				Visibility.ANY);

		@Override
		public <T> T toObject(String pSrc, Class<T> pType) throws Exception {
			return Jack.readValue(pSrc, pType);
		}

		@Override
		public String toString(Object pObj) throws Exception {
			return Jack.writeValueAsString(pObj);
		}
	};

	public static void main(String[] args) throws Exception {

		// create a Jamn server
		JamnServer lServer = new JamnServer();
		lServer.getConfig().setCORSEnabled(true); // required for localhost communication via js fetch

		// set logging and json tool for the JamnWebRPCProvider
		JamnWebRPCProvider.setLogger(lServer.getLoggerFor(JamnWebRPCProvider.class.getName()));
		JamnWebRPCProvider.setJsonTool(Jackson);
		//create the RPC provider
		JamnWebRPCProvider lWebRPCProvider = new JamnWebRPCProvider();

		// register your RPC-API Services
		lWebRPCProvider.registerApiService(SampleWebApiServices.class);
		
		//get the actual provider and add it to the server
		lServer.addContentProvider("RPCProvider", lWebRPCProvider.getJamnContentProvider());
		//start server
		lServer.start();
	}
}
