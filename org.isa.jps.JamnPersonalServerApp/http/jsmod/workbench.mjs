/* Authored by www.integrating-architecture.de */

import { ServerOrigin, setVisibility } from '../jsmod/tools.mjs';
import { WorkbenchViewManager }  from '../jsmod/view-manager.mjs';
import { ConfirmationDialog }  from '../jsmod/view-classes.mjs';
import * as websocket from '../jsmod/websocket.mjs';
import * as sidebar from '../jsmod/sidebar.mjs';
import * as sidebarContent from '../jsmod/sidebar-content.mjs';
import * as systemInfos from '../jsmod/system-infos.mjs';
import * as loginModule from '../jsmod/login.mjs';

/**
 * The workbench module implements the toplevel Single-Page-Application.
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
				throw new Error("The global name [" + globalName + "] is already defined");
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
 * for use on document level -> see registerAs -> globalName  
 */
export const WorkbenchInterface = {

	confirm: (text, cb) => {
		confirmationDialog.open(text, cb);
	},

	//public view action request
	onViewAction: (evt, action) => {
		viewManager.onViewAction(evt, action);
	},
	
	sendWsoMessage : (wsoMsg, sentCb=null) => {
		return websocket.sendMessage(wsoMsg, sentCb);
	},
	
	addWsoMessageListener : (cb) => {
		websocket.addMessageListener(cb);
	}
};

/**
 * Internals
 */
let rootElement = null;
let workarea = document.getElementById("workarea");
let modalDialog = document.getElementById("modal.dialog");
let viewManager = new WorkbenchViewManager(workarea, modalDialog);
let confirmationDialog = new ConfirmationDialog();



/**
 * this is called after document load but before getting visible
 */
function startApp() {
	initWebSocket();
	initUI();
	
	initFunctionalSidebarItems();

	//set the app visible
	setVisibility(rootElement, true);

}

/**
 */
function initFunctionalSidebarItems() {

	let startLogin = (evt) => {
		evt.stopImmediatePropagation();
		viewManager.getModalDialog(loginModule.getView(), (dlg) => {
			loginModule.processSystemLogin(dlg);
		 });
	}
	//sidebar system login item
	let item = document.getElementById("sidebar.system.login");
	item.addEventListener("click", startLogin);

	//sidebar header login icon
	item = document.getElementById("sidebar.header.login.icon");
	item.addEventListener("click", startLogin);
	
}

/**
 */
function initWebSocket() {
	let wsodata = {};	
	wsodata.hostUrl =  ServerOrigin("wsoapi");
	
	websocket.initialize(wsodata);
	websocket.connect();
}

/**
 */
function initUI() {
	let topic = null;
	let topicDef = null;
	let topicKey = null;
	let itemDef = null;
	let itemKey = null;
	
	//create the sidebar content from the - sidebar-content.mjs definitions
	for(topicKey in sidebarContent.topicList){
		topicDef = sidebarContent.topicList[topicKey];
		topic = sidebar.createTopic(topicKey, sidebar.newTopicHtml(topicDef.icon, topicDef.title));
		for(let key in topicDef.items){
			itemKey = topicKey+"_"+key;
			itemDef = topicDef.items[key];
			if(itemDef?.view){
				itemDef.view.onInstallation(itemKey, itemDef?.data, viewManager);
				viewManager.registerView(itemDef.view, itemDef?.data);
				topic.addItem(sidebar.newtItemHtml(itemDef.view.id, itemDef.title));
			} else if(itemDef?.id){
				topic.addItem(sidebar.newIdentifiableItemHtml(itemDef.id, itemDef.title));
			}
		}
	}

	sidebar.setItemAction((id)=>viewManager.onComponentOpenViewRequest(sidebar, id));
	sidebar.build();
	
	//set statusline github icon href
	systemInfos.getInfos((data)=>{
		let elem = document.getElementById("statusLineScmLink");
		elem.setAttribute("href", data.links["app.scm"]);
	});
}

