/* Authored by www.integrating-architecture.de */

import { CommandDef } from '../jsmod/data-classes.mjs';
import { getView as systemInfosView } from '../jsmod/system-infos.mjs';
import { getView as commandView } from '../jsmod/command.mjs';
import { getView as dbConnectionsView } from './db-connections.mjs';

/**
 * <pre>
 * Data object that defines the content of the Workbench Sidebar.
 * The structure is:
 * n Topics 
 *  - 1 Topic -> n Items
 * The keys must be unique on their level.
 * 
 * </pre>
 */
export const topicList = {
	system : {icon:"system", title:"&ensp;System",
		items : {
			//create a view item 
			"infos" : {title:"Infos", view:systemInfosView()},
			
			//create a functional item with just an id
			//that gets explicite wired by the app
			"login" : {title:"Login", id:"sidebar.system.login"}
		}
	},
	
	commands : {icon:"command", title:"&ensp;Commands",
		items : {
			//create view item with data
			"testSample" : {title:"Sample: shell test", view:commandView(), data:new CommandDef("Sample: [test sh command]", "runjs", "/sample/sh-test.mjs", {args:true})},
			"projectSample" : {title:"Sample: build test", view:commandView(), data:new CommandDef("Sample: [test build script]", "runjs", "/sample/build-project-test.mjs")},
			"runjcSample" : {title:"Sample: Java cmd", view:commandView(), data:new CommandDef("Sample Java Command: [org.xyz.sample.JavaCommand]", "runjc", "org.xyz.sample.JavaCommand")}
		}
	},
	
	tools : {icon:"tools", title:"&ensp;Tools",
		items : {
			"dbconnections" : {title:"DB Connections", view:dbConnectionsView()}
		}
	}
}

