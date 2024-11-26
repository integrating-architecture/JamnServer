/*jamn.web.template*/

/**
 * A sample js app module
 */
export function anchorAt (rootId) {
	rootElement = document.getElementById(rootId);
	document.addEventListener("DOMContentLoaded", startApp);
}

//this will get injected on the server side
export const ServerInfo = ${server.info};

/**
 * Private section
 */
let rootElement = null;

//this is called after document load but before getting visible
function startApp () {
	prepare();
	show();
}

function prepare () {
	setHTML("server.name", ServerInfo.name);
	setHTML("server.version", ServerInfo.version);
	setHTML("server.description", ServerInfo.description);
}

function show () {
	rootElement.style.visibility = 'visible';
}

function setHTML (id, html){
	document.getElementById(id).innerHTML = html;
}
