/*jamn.web.template*/

import * as tool from '/jsmod/tools.mjs';

/**
 * Public
 */
//the view's dom element id
export const viewId = "systemInfoView";

//add the view to the workarea
export function open() {
	getViewHtml((html)=>{
		showView(html);
	});
}

//remove the view from the workarea
export function close() {
	tool.setHTML("workarea", "");
}

export function getInfos(cb){
	if (infos) {
		cb(infos);
	} else {
		//load the infos from server
		tool.callWebService("/api/system-infos").then((data) => {
			infos = data;
			cb(infos);
		});
	}	
}

/**
 * Internals
 */
let viewHtml = null;
let infos = null;

//get the view html 
function getViewHtml(cb) {
	if (viewHtml) {
		cb(viewHtml);
	} else {
		//load the html from server
		tool.fetchPlainText("/jsmod/system-infos.html").then((html) => {
			viewHtml = html;
			cb(viewHtml);
		});
	}
}

function showView(html) {
	tool.setHTML("workarea", html);
	setData();
	tool.setStyle(viewId, "visibility", "visible");
}

function setData() {
	getInfos((data)=>{
		tool.setChildHTML(viewId, "server.name", data.name);
		tool.setChildHTML(viewId, "server.version", data.version);
		tool.setChildHTML(viewId, "server.description", data.description);		
	});
}
