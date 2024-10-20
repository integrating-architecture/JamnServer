package org.isa.ipc.sample;

import java.util.Map;

import org.isa.ipc.JamnServer;
import org.isa.ipc.JamnWebRPCProvider;
import org.isa.ipc.web.api.About;

public class WebRPCTest {

	public static void main(String[] args) throws Exception{
		JamnServer lServer = new JamnServer();
		lServer.getConfig().setCORSEnabled(true); // required for localhost communication via js fetch

		JamnWebRPCProvider lRPCProvider = new JamnWebRPCProvider(lServer.getLoggerFor(JamnWebRPCProvider.class.getName()));
		lRPCProvider.registerApiService(About.class);
		
		//adding the new content provider
		//and active it through the provider dispatcher		
		String lProviderID = lRPCProvider.getClass().getName();
		lServer.addContentProvider(lProviderID, lRPCProvider.getJamnContentProvider());
		lServer.setContentProviderDispatcher(new JamnServer.ContentProviderDispatcher() {
			@Override
			public String getContentProviderIDFor(Map<String, String> pRequestAttributes) {
				return lProviderID;
			}});

		lServer.start();
	}

}
