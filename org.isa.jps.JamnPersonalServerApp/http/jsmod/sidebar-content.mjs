/* Authored by iqbserve.de */

import { CommandDef } from '../jsmod/data-classes.mjs';
import { getView as getSystemInfosView } from '../jsmod/system-infos.mjs';
import { getView as getDbConnectionsView } from '../jsmod/db-connections.mjs';
import { getView as newCmdView } from '../jsmod/command.mjs';
import { processSystemLogin as loginAction } from '../jsmod/login.mjs';
import * as Icons from '../jsmod/icons.mjs';


/**
 * <pre>
 * Data object that defines the content of the Workbench Sidebar.
 * The structure is:
 * n Topics 
 *  - 1 Topic 
 *    - n Items
 * </pre>
 */
export const topicList = {
	system: {
		iconName: Icons.system(), title: "System",
		items: [
			//create a view item with a singleton view
			{ title: "Infos", view: getSystemInfosView() },

			//create a functional item with id and an action
			{ title: "Login", id: "sidebar.system.login", action: loginAction }
		]
	},

	commands: {
		iconName: Icons.command(), title: "Commands",
		items: [
			//create new views from the same type with identifying names and data objects 
			{
				title: "Sample: shell test",
				view: newCmdView("shellSampleView"),
				data: new CommandDef("Sample: [sh command]", "runjs", "/sample/sh-test.mjs", { args: true })
			},
			{
				title: "Sample: build test",
				view: newCmdView("buildSampleView"),
				data: new CommandDef("Sample: [build script]", "runjs", "/sample/build-project-test.mjs")
			},
			{
				title: "Sample: extension",
				view: newCmdView("extensionSampleView"),
				data: new CommandDef("Sample: [extension command]", "runext", "sample.Command", { args: true })
			}
		]
	},

	tools: {
		iconName: Icons.tools(), title: "Tools",
		items: [
			{ title: "DB Connections", view: getDbConnectionsView() }
		]
	}
}

