/* Authored by www.integrating-architecture.de */

import { ServerOrigin, setVisibility, getWorkViewOf, getViewHtml } from '../jsmod/tools.mjs';
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
	
	registeredViews={};
	
	getAsCartridgeId = (viewId)=>"view.cartridge."+viewId;
	
	registerView(view, viewData){
		this.registeredViews[view.id] = {view: view, data:viewData, cart:null};
	}

	setViewCartVisible(viewCart, flag) {
		if(flag){
			viewCart.style = "visibility: visible; display: block;"
		}else{
			viewCart.style.display = "none"	
		}
	}

	createViewCartridge(viewId, html){
		let viewCart = document.createElement("div");
		viewCart.id = this.getAsCartridgeId(viewId);
		viewCart.style = "visibility: visible; display: block;"
		viewCart.innerHTML = html;
		viewCart.children[0].id = viewId;
		this.registeredViews[viewId].cart = viewCart;
		return viewCart;
	}

	closeAllCloseableViews(){
		for(let key in this.registeredViews){
			let viewItem = this.registeredViews[key];
			if(viewItem.cart){
				this.closeView(viewItem);
			}
		}
	}

	closeView(viewItem){
		// view is expected handle close itself 
		// and return true if it was closeabel and did close
		if(viewItem.view.close()){
			this.setViewCartVisible(viewItem.cart, false);
		}
	}

	openView(viewItem) {
		this.closeAllCloseableViews();

		viewItem.view.open();
		this.setViewCartVisible(viewItem.cart, true);
	}
		
	//default action requests 
	onViewAction(evt, action) {
		let workView = getWorkViewOf(evt.target);
		let viewItem = this.registeredViews[workView.id];

		if(!viewItem){
			throw new Error(`UNKNOWN WorkView [${workView.id}]`);
		}
		
		if("close"===action){
			this.closeView(viewItem);
		}else if("pin"===action){
			viewItem.view.togglePinned();
		}else if("collapse"===action){
			viewItem.view.toggleCollapsed();
		}	

		evt.stopImmediatePropagation();
	}
	
	// ViewManager public view open request method for components
	// in this case the sidebar
	onComponentOpenViewRequest(comp, viewItemId) {
		let viewItem = this.registeredViews[viewItemId]
		 
		if(viewItem){
			if(viewItem.cart){
				this.openView(viewItem);				
			}else{
				getViewHtml(viewItem.view.viewSource, (html)=>{
					let viewCart = this.createViewCartridge(viewItem.view.id, html);
					workarea.append(viewCart);
					this.openView(viewItem);
				});
			}
		}
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
				itemDef.view.onInstallation(itemKey, itemDef?.data);
				viewManager.registerView(itemDef.view, itemDef?.data);
				topic.addItem(sidebar.newtItemHtml(itemDef.view.id, itemDef.title));
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

