/* Authored by www.integrating-architecture.de */

import { ServerOrigin, setVisibility } from '../jsmod/tools.mjs';
import * as websocket from '../jsmod/websocket.mjs';
import * as sidebar from '../jsmod/sidebar.mjs';
import * as sidebarContent from '../jsmod/sidebar-content.mjs';
import * as systemInfos from '../jsmod/system-infos.mjs';

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

	//public close request from anywhere
	//e.g. onclick="WbApp.onViewClose(event)" see view html
	onViewClose: (evt) => {
		viewManager.onViewClose(evt);
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
let registeredSidebarItems = {};
let workarea = document.getElementById("workarea");

/**
 * A simple manager to handle dom creation/manipulation
 * for the on demand view html loading
 * and the workarea display logic.
 * 
 * A view element is instantiated as the child of a view catridge div
 * that becomes added to the workbench-workarea.
 * 
 * The manager handles the view catriges while the view is responsible for itself.
 */
class WorkbenchViewManager {
	
	currentView = null;
	catridgeId = (viewId)=>"view.cartridge."+viewId;
	
	getViewCatridge(viewId){
		return document.getElementById(this.catridgeId(viewId));
	}

	createViewCatridge(viewId, html){
		let viewCat = document.createElement("div");
		viewCat.id = this.catridgeId(viewId);
		viewCat.style = "visibility: visible; display: block;"
		viewCat.innerHTML = html;
		viewCat.children[0].id = viewId;
		return viewCat;
	}

	open(view, html) {
		let viewCat = this.getViewCatridge(view.id);
		if(!viewCat){
			viewCat = this.createViewCatridge(view.id, html);
			workarea.append(viewCat);			
		}else{
			viewCat.style = "visibility: visible; display: block;"
		}
	}

	close(view) {
		let viewCat = this.getViewCatridge(view.id);
		viewCat.style.display = "none"
	}
	
	onSidebarOpenView(id) {
		let itemDef = registeredSidebarItems[id];
		 
		if(itemDef?.view){
			//ask view if it can be closed
			if(this.currentView?.isCloseable(itemDef)){
				this.currentView.close();
				this.currentView = itemDef.view;		
				this.currentView.open(itemDef.data);
			}else if(!this.currentView){
				//if no view is present
				this.currentView = itemDef.view;		
				this.currentView.open(itemDef.data);
			}
		}
	}
	
	//close request from anywhere
	onViewClose(evt) {
		if (this.currentView) {
			//view decides if it can be closed
			if(this.currentView.close()){
				this.currentView = null;				
			}
		}
		evt.stopImmediatePropagation();
	}
}

let viewManager = new WorkbenchViewManager();

/**
 * this is called after document load but before getting visible
 */
function startApp() {
	initWebSocket();
	initUI();
	
	//set the app visible
	setVisibility(rootElement, true);
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
		topic = sidebar.createTopic(sidebar.newTopicHtml(topicDef.icon, topicDef.title));
		for(let key in topicDef.items){
			itemKey = topicKey+"_"+key;
			itemDef = topicDef.items[key];
			if(itemDef?.view){
				//set this view manager to all views
				itemDef.view.setViewManager(viewManager);
				itemDef.view.onInstallation(itemKey, itemDef?.data);
			}
			registeredSidebarItems[itemKey] = itemDef;
			topic.addItem(sidebar.newtItemHtml(itemKey, itemDef.title));
		}
	}

	sidebar.setItemAction((id)=>viewManager.onSidebarOpenView(id));
	sidebar.build();
	
	//set statusline github icon href
	systemInfos.getInfos((data)=>{
		let elem = document.getElementById("statusLineScmLink");
		elem.setAttribute("href", data.links["app.scm"]);
	});
}

