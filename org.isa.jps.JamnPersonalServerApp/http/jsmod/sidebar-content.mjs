/* Authored by www.integrating-architecture.de */

import { CommandDef } from '../jsmod/data-classes.mjs';
import { getView as systemInfosView } from '../jsmod/system-infos.mjs';
import { getView as commandView } from '../jsmod/command.mjs';

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
	system : {icon:"bi bi-laptop", title:"&ensp;System",
		items : {
			"infos" : {title:"Infos", view:systemInfosView(), data:null}
		}
	},
	
	commands : {icon:"bi bi-command", title:"&ensp;Commands",
		items : {
			"testSample" : {title:"Sample Test command", view:commandView(), data:new CommandDef("Sample Test command", "runjs", "/sample/test.mjs", "<none>")},
			"projectSample" : {title:"Sample Build", view:commandView(), data:new CommandDef("Sample Project Build", "runjs", "/sample/build-project.mjs", "<none>")}
		}
	},
	
	tools : {icon:"bi bi-tools", title:"&ensp;Tools",
		items : {
			
		}
	}
}

