/*jamn.web.template*/

import { initSideBar } from '/jsmod/wb-sidebar.mjs';

//this data will get injected on the server side
export const ServerInfo = ${ server.info };

/**
 */
export function anchorAt(rootId) {
	rootElement = document.getElementById(rootId);
	document.addEventListener("DOMContentLoaded", startApp);
	
	window.wbapp = WorkbenchAppInterface;
};

/**
 */
export function setSidebarTopicIds(ids) {
	topicIds = ids;
};

export const WorkbenchAppInterface = {
	log : function (msg) {
		console.log(msg);
	},
	
	test : function (e){
		console.log(e);
	}
};
 
/**
 * Private section
 */
let rootElement = null;
let topicIds = [];

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
	initSideBar(topicIds);

	setHTML("server.name", ServerInfo.name);
	setHTML("server.version", ServerInfo.version);
	setHTML("server.description", ServerInfo.description);

	setAttr("app.scm", "href", ServerInfo.links["app.scm"]);
}

/**
 */
function show() {
	rootElement.style.visibility = 'visible';
}

/**
 * Helper
 */
/**
 */
function setHTML(id, html) {
	document.getElementById(id).innerHTML = html;
}

/**
 */
function setAttr(id, name, value) {
	document.getElementById(id).setAttribute(name, value);
}
