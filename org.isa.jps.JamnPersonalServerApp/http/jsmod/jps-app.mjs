/*jamn.web.template*/

/**
 * A sample js app module
 */
export function anchorAt (rootId) {
	rootElement = document.getElementById(rootId);
	document.addEventListener("DOMContentLoaded", startApp);
}


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
}

function show () {
	rootElement.style.visibility = 'visible';
}

export function setHTML (id, html){
	document.getElementById(id).innerHTML = html;
}

function setAttr (id, name, value){
	document.getElementById(id).setAttribute(name, value);
}
