/* Authored by www.integrating-architecture.de */

import { ServerOrigin, setVisibility } from '../jsmod/tools.mjs';
import { WorkbenchViewManager } from '../jsmod/view-manager.mjs';
import { StandardDialog, IconElement, SplitBarHandler, ViewComp } from '../jsmod/view-classes.mjs';
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

	confirm: (text, cb) => {
		standardDlg.openConfirmation(text, cb);
	},

	//public view action request
	onViewAction: (evt, action) => {
		viewManager.onViewAction(evt, action);
	},

	sendWsoMessage: (wsoMsg, sentCb = null) => {
		return websocket.sendMessage(wsoMsg, sentCb);
	},

	addWsoMessageListener: (cb) => {
		websocket.addMessageListener(cb);
	},

	statusLineInfo: (info) => {
		statusLineInfo.innerHTML = info;
	},

	titleInfo: (info) => {
		titleInfo.innerHTML = `[ ${info} ]`;
	},
};

/**
 * Internals
 */
let rootElement = null;
let workarea = document.getElementById("workarea");
let modalDialog = document.getElementById("modal.dialog");
let viewManager = new WorkbenchViewManager(workarea, modalDialog);

let standardDlg = new StandardDialog();

let statusLineInfo = null;
let titleInfo = null;
let systemData = null;

/**
 * this is called after document load but before getting visible
 */
function startApp() {

	systemInfos.getInfos((data) => {
		systemData = data;
		initWebSocket();
		initUI();
		setVisibility(rootElement, true);
	});
}

/**
 */
function initWebSocket() {
	let wsodata = {};
	wsodata.hostUrl = ServerOrigin("wsoapi");

	websocket.initialize(wsodata);
	websocket.connect();
}

/**
 */
function initUI() {

	//document.onclick = (evt) => { WorkbenchInterface.statusLineInfo(""); };

	initTitlebar();
	initSidebar();
	initStatusline();
	initIntroBox();

}

/**
 */
function initSidebar() {
	let topic = null;
	let topicDef = null;
	let topicKey = null;
	let itemDef = null;
	let itemKey = null;

	//create the sidebar content from the - sidebar-content.mjs definitions
	for (topicKey in sidebarContent.topicList) {
		topicDef = sidebarContent.topicList[topicKey];
		topic = sidebar.createTopic(topicKey, sidebar.newTopicHtml(topicDef.icon, topicDef.title));
		for (let key in topicDef.items) {
			itemKey = topicKey + "_" + key;
			itemDef = topicDef.items[key];
			if (itemDef?.view) {
				itemDef.view.onInstallation(itemKey, itemDef?.data, viewManager);
				viewManager.registerView(itemDef.view, itemDef?.data);
				topic.addItem(sidebar.newtItemHtml(itemDef.view.id, itemDef.title));
			} else if (itemDef?.id) {
				topic.addItem(sidebar.newIdentifiableItemHtml(itemDef.id, itemDef.title));
			}
		}
	}

	sidebar.setItemAction((id) => viewManager.onComponentOpenViewRequest(sidebar, id));
	sidebar.build();

	sidebar.initFunctionalItems(viewManager);

	//init splitter
	let splitter = new SplitBarHandler(
		document.getElementById("sidebar.splitter"),
		document.getElementById("sidebar"),
		document.getElementById("workarea")
	)
	splitter.barrierActionBefore = (splitter, val) => {
		//sidebar width < x - collaps it
		if (val < 100) {
			splitter.stop();
			sidebar.toogleCollaps();
			return true; //barrier hit
		}
		return false; //barrier NOT hit
	}
}

/**
 */
function initTitlebar() {

	titleInfo = document.getElementById("wtb.title.text");
	WorkbenchInterface.titleInfo(`Tiny Demo - V.${systemData.version}`);

	ViewComp.newFor(document.getElementById("wtb.ctrl.panel"))
		.addActionIcon({ iconName: "caretup", title: "Backward step through views" }, (target) => {
			target.icon.onclick = () => {
				viewManager.stepViewsUp();
			}
		})
		.addActionIcon({ iconName: "caretdown", title: "Forward step through views" }, (target) => {
			target.icon.onclick = (evt) => {
				viewManager.stepViewsDown();
				evt.stopPropagation();
			}
		});
}

/**
 */
function initStatusline() {

	statusLineInfo = document.getElementById("wsl.info");

	let scmIcon = IconElement.newIcon("github", document.getElementById("wsl.scm.link"));
	scmIcon.elem.setAttribute("href", systemData.links["app.scm"]);
}

/**
 */
function initIntroBox() {

	let data = systemData.buildInfos;
	let scmIcon = IconElement.iconDef("github")[0];

	let introContentHtml = `
		<span style="padding: 20px;">
			<h1 style="color: var(--standard-dlg-header-bg)">Welcome to<br>Jamn Workbench</h1>
			<span style="font-size: 18px;">
			<p>an example of using the Jamn Java-SE Microservice<br>together with plain Html and JavaScript<br></p>
			<p style="margin-bottom: 5px;">to build lightweight, browser enabled
				<a class="${scmIcon}" style="color: var(--isa-title-blue);" title="Jamn All-In-One MicroService"
				target="_blank" href="${data["readme.url"]}"><span style="margin-left: 5px;">All-in-One Apps</span></a>
			</p>
			<a style="font-size: 10px; color: var(--isa-title-blue);" 
			href="${data["author.url"]}" title="${data["author"]}" target="_blank">${data["author"]}</a>
			</span>
		</span>
		<!---->
		<span>
			<img src="images/intro.jpg" alt="Intro" style="width: 350px; height: 100%;">
		</span>
	`;

	document.getElementById("intro.content").innerHTML = introContentHtml;

	let intro = document.getElementById("intro.overlay");
	intro.onclick = () => { intro.style.display = "none" };
}
