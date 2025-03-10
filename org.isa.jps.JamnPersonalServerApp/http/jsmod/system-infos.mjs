/* Authored by www.integrating-architecture.de */

import { callWebService } from '../jsmod/tools.mjs';
import { WorkView } from '../jsmod/view-classes.mjs';

/**
 * Concrete view class for this info component
 */
class SystemInfoView extends WorkView {
		
	initialize() {	
		super.initialize();
		this.setTitle("System Infos");
		
		elem.name = this.getElement("server.name");
		elem.version = this.getElement("server.version");
		elem.description = this.getElement("server.description");
				
		this.isInitialized = true;
	}
	
	writeDataToView(){
		getInfos((data)=>{
			elem.name.innerHTML = data.name;
			elem.version.innerHTML = data.version;
			elem.description.innerHTML = data.description;
		});
	}	
}

//export this view component as singleton instance
const viewInstance = new SystemInfoView("systemInfoView", "/jsmod/system-infos.html");
export function getView(){
	return viewInstance;
} 

// using module scope for data handling
// avoids the class "this"" boilerplate
let elem = {
	name : null,
	version : null,
	description : null
};

let infos = null;

/**
 */
export function getInfos(cb){
	if (infos) {
		cb(infos);
	} else {
		//load the infos from server
		callWebService("/api/system-infos").then((data) => {
			infos = data;
			cb(infos);
		});
	}	
}
