/*jamn.web.template*/

import * as tool from '/jsmod/tools.mjs';
import * as sidebar from '/jsmod/wb-sidebar.mjs';
import * as systemInfos from '/jsmod/system-infos.mjs';

/**
 * Public
 */

/**
 * Browser-Document registration function
 */
export function anchorAt(rootId) {
	rootElement = document.getElementById(rootId);

	return {
		registerAs: function (globalName) {
			if (!(globalName in window)) {
				window[globalName] = WorkbenchInterface;
				console.log("Workbench App globally registered as [" + globalName + "]");
			} else {
				throw "The global name [" + globalName + "] is already defined";
			}
			return this;
		},

		build: function () {
			document.addEventListener("DOMContentLoaded", startApp);
		}
	};
};

/**
 * A public NONE module function interface export
 * e.g. for use on document level -> see registerAs -> globalName  
 */
export const WorkbenchInterface = {

	openView: (id) => {
		if (currentView == null && registeredViews[id]) {
			currentView = registeredViews[id];
			currentView.open();
		}
	},

	onViewClose: (evt) => {
		if (currentView) {
			currentView.close();
			currentView = null;
		}
		evt.stopImmediatePropagation();
	}
};

/**
 * Internals
 */
//provide all known/imported view modules
//as properties of the object registeredViews
const registeredViews = {
	"systemInfos": systemInfos
};

let rootElement = null;
let currentView = null;

/**
 * this is called after document load but before getting visible
 */
function startApp() {
	initialize();
	show();
}

/**
 */
function initialize() {
	
	// create sidebar programmatically
	// the code is a playful attempt
	// the sidebar topic list can just as easy be written directly in HTML
	sidebar.createTopic(sidebar.newTopicHtml("bi bi-laptop", "&ensp;System"))
		.addItem(sidebar.newtItemHtml("systemInfos", "Infos"))
		.addItem(sidebar.newtItemHtml("systemProperties", "Properties"));
	
	sidebar.createTopic(sidebar.newTopicHtml("bi bi-command", "&ensp;Commands"));
	sidebar.createTopic(sidebar.newTopicHtml("bi bi-tools", "&ensp;Tools"));
	
	sidebar.setItemAction((id)=>WorkbenchInterface.openView(id));
	sidebar.initialize();
	
	//set statusline github icon href
	systemInfos.getInfos((data)=>{
		tool.setAttr("statusLineScmLink", "href", data.links["app.scm"]);
	});
}

/**
 */
function show() {
	rootElement.style.visibility = 'visible';
}
