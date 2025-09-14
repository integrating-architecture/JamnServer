/* Authored by iqbserve.de */

import { ServerOrigin, setDisplay, setVisibility } from '../jsmod/tools.mjs';
import { WorkbenchViewManager } from '../jsmod/view-manager.mjs';
import { StandardDialog, SplitBarHandler, ViewBuilder, onClicked } from '../jsmod/view-classes.mjs';
import * as websocket from '../jsmod/websocket.mjs';
import * as sidebar from '../jsmod/sidebar.mjs';
import * as systemInfos from '../jsmod/system-infos.mjs';
import * as Icons from '../jsmod/icons.mjs';
import { WbProperties } from '../jsmod/workbench-properties.mjs';


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
			console.log("Workbench App installed");
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

	modalDialog: (view, cb) => {
		viewManager.getModalDialog(view, cb);
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
	}
};

/**
 * Internals
 */
let rootElement = null;
let workarea = document.getElementById("workarea");
let modalDialog = document.getElementById("modal.dialog");

let standardDlg = new StandardDialog();

let statusLineInfo = null;
let titleInfo = null;
let systemData = null;

let viewManager = new WorkbenchViewManager(workarea, modalDialog);


/**
 * this is called after document load but before getting visible
 */
function startApp() {

	systemInfos.getInfos((data) => {
		systemData = data;
		initWebSocket();
		initUI();

		setVisibility(rootElement, true);

		if (WbProperties.autoStartView) {
			viewManager.onComponentOpenViewRequest(WbProperties.autoStartView);
		}
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

	initTitlebar();
	initSidebar();
	initStatusline();
	initIntroBox();

}

/**
 */
function initSidebar() {

	sidebar.initialize(viewManager);

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
			sidebar.toggleCollaps();
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

	new ViewBuilder().newViewCompFor(document.getElementById("wtb.ctrl.panel"))
		.addActionIcon({ iconName: Icons.caretup(), title: "Backward step through views" }, (target) => {
			onClicked(target.icon, () => { viewManager.stepViewsUp(); });
		})
		.addActionIcon({ iconName: Icons.caretdown(), title: "Forward step through views" }, (target) => {
			onClicked(target.icon, () => { viewManager.stepViewsDown(); });
		});
}

/**
 */
function initStatusline() {

	statusLineInfo = document.getElementById("wsl.info");

	Icons.github(document.getElementById("wsl.scm.link")).init((icon) => {
		icon.elem.href = systemData.links["app.scm"];
	});

}

/**
 */
function initIntroBox() {

	let intro = document.getElementById("intro.overlay");

	if (!WbProperties.showIntro) {
		setDisplay(intro, false);
		return;
	};

	onClicked(intro, (evt) => {
		setDisplay(evt.currentTarget, false);
	});

	let data = systemData.buildInfos;

	document.getElementById("intro.content").innerHTML = `
		<span style="padding: 20px;">
			<h1 style="color: var(--isa-title-grayblue)">Welcome to<br>Jamn Workbench</h1>
			<span style="font-size: 18px;">
			<p>an example of using the Jamn Java-SE Microservice<br>together with plain Html and JavaScript<br></p>
			<p style="margin-bottom: 5px;">to build lightweight, browser enabled
				<a class="${Icons.getIconClasses("github", true)}" style="color: var(--isa-title-blue);" title="Jamn All-In-One MicroService"
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
}
