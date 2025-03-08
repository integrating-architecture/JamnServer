/* Authored by www.integrating-architecture.de */

import { NL, getChildOf, setVisibility, setDisplay } from '../jsmod/tools.mjs';
import { ViewSource, CommandDef } from '../jsmod/data-classes.mjs';

/**
 * View base classes.
 * 
 * View classes encapsulate a basic structure and a basic open close procedure 
 * in the form of corresponding methods and flags.
 * 
 */

export class GeneralView {

	id = "";
	viewSource = new ViewSource("");
	viewManager = null;

	//the view html dom element
	viewElement = null;
	viewTitle = null;
	viewWorkarea = null;

	isInitialized = false;
	isOpen = false;

	constructor(id, file) {
		this.id = id;
		this.viewSource = new ViewSource(file);

		this.isInitialized = false;
		this.isOpen = false;
	}

	setViewManager(viewManager) {
		this.viewManager = viewManager;
	}

	open(data = null) {
		if (!this.isInitialized) {
			this.initialize();
		}
		this.writeDataToView();
		setVisibility(this.viewElement, true);
		this.isOpen = true;
	}

	close() {
		this.isOpen = false;
	}

	initialize() {
		//to be overwritten
		//only called when isInitialized=false
		this.viewElement = document.getElementById(this.id);
		this.viewTitle = this.getElement("view.title");
		this.viewWorkarea = this.getElement("work.view.workarea");
	}

	writeDataToView() {
		//to be overwritten
		//always called in showView
	}

	readDataFromView() {
		//to be overwritten
	}

	setTitle(title) {
		this.viewTitle.innerHTML = title;
	}

	getElement(id) {
		return getChildOf(this.viewElement, id);
	}

}


export class WorkView {

	id = "";
	viewSource = new ViewSource("");
	viewManager = null;

	//the view html dom element
	viewElement = null;
	viewTitle = null;
	viewWorkarea = null;

	//header elements
	closeIcon = null;
	collapseIcon = null;
	pinIcon = null;

	headerMenu = null;

	isInitialized = false;
	isRunning = false;
	isOpen = false;
	isPinned = false;
	isCollapsed = false;

	constructor(id, file) {
		this.id = id;
		this.viewSource = new ViewSource(file);

		this.isInitialized = false;
		this.isRunning = false;
		this.isOpen = false;
	}

	open(data = null) {
		if (!this.isInitialized) {
			this.initialize();
		}
		if (this.headerMenu) {
			this.headerMenu.close();
		}
		this.writeDataToView();
		setVisibility(this.viewElement, true);
		this.isOpen = true;
	}

	close() {
		this.isOpen = false;
		if (this.headerMenu) {
			this.headerMenu.close();
		}
		return this.isCloseable();
	}

	isCloseable(ctxObj = null) {
		return !(this.isRunning || this.isPinned);
	}

	initialize() {
		//to be overwritten
		//only called when isInitialized=false
		this.viewElement = document.getElementById(this.id);
		this.viewTitle = this.getElement("view.title");
		this.viewWorkarea = this.getElement("work.view.workarea");

		this.closeIcon = this.getElement("close.icon");
		this.pinIcon = this.getElement("pin.icon");
		this.collapseIcon = this.getElement("collapse.icon");

		let elem = this.getElement("header.menu");
		if (elem && this.closeIcon) {
			this.headerMenu = new WorkViewHeaderMenu(elem);

			this.headerMenu.addItem("Close", (evt) => {
				this.closeIcon.click();
			}, { separator: "bottom" });

			if (this.viewManager) {
				this.headerMenu.addItem("Move up", (evt) => {
					this.viewManager.moveView(this, "up");
				});
				this.headerMenu.addItem("Move down", (evt) => {
					this.viewManager.moveView(this, "down");
				});
				this.headerMenu.addItem("Move to ...", (evt) => {
					this.viewManager.promptUserInput({ title: "", message: "Please enter your desired position number:" }, "1",
						(value) => value ? this.viewManager.moveView(this, value) : null
					);
				});
			}
		}
	}

	writeDataToView() {
		//to be overwritten
		//always called in showView
	}

	readDataFromView() {
		//to be overwritten
	}

	setRunning(flag) {
		this.isRunning = flag;
	}

	setTitle(title) {
		this.viewTitle.innerHTML = title;
	}

	getElement(id) {
		return getChildOf(this.viewElement, id);
	}

	onInstallation(installKey, installData, viewManager) {
		this.viewManager = viewManager;
	}

	togglePinned() {
		this.isPinned = !this.isPinned;

		if (this.isPinned) {
			this.pinIcon.classList.remove("bi-pin");
			this.pinIcon.classList.add("bi-pin-angle");
			this.pinIcon.title = "Unpin view";
			this.closeIcon.style["pointer-events"] = "none";
		} else {
			this.pinIcon.classList.remove("bi-pin-angle");
			this.pinIcon.classList.add("bi-pin");
			this.pinIcon.title = "Pin to keep view";
			this.closeIcon.style["pointer-events"] = "all";
		}

		return this.isPinned;
	}

	toggleCollapsed() {
		this.isCollapsed = !this.isCollapsed;

		if (this.isCollapsed) {
			this.collapseIcon.classList.remove("bi-chevron-bar-contract");
			this.collapseIcon.classList.add("bi-chevron-bar-expand");
			this.collapseIcon.title = "Expand view";
		} else {
			this.collapseIcon.classList.remove("bi-chevron-bar-expand");
			this.collapseIcon.classList.add("bi-chevron-bar-contract");
			this.collapseIcon.title = "Collapse  view";
		}

		setDisplay(this.viewWorkarea, !this.isCollapsed);

		return this.isCollapsed;
	}

	toggleHeaderMenu() {
		if (!this.isCollapsed) {
			this.headerMenu.toggleVisibility();
		}
	}

}


/**
 * A Base class for command views
 * that get parameterized with a commandDef object on installation
 */
export class BaseCommandView extends WorkView {

	commandDef = new CommandDef();
	commandName = "";

	runButton = null;
	runArgs = null;
	workIndicator = null;

	outputArea = null;

	onInstallation(installKey, installData, viewManager) {
		super.onInstallation(installKey, installData, viewManager);
		this.id = installKey;
		if (installData instanceof CommandDef) {
			this.commandDef = installData;
			this.commandName = this.commandDef.command + " " + this.commandDef.script;
		}
	}

	initialize() {
		super.initialize();

		this.setTitle(this.commandDef.title);

		this.runButton = this.getElement("pb.run");
		this.workIndicator = this.getElement("work.indicator");
		this.runArgs = this.getElement("cmd.args");
		this.outputArea = this.getElement("cmd.output");

		if (this.headerMenu) {
			this.headerMenu.addItem("Clear Output", (evt) => {
				this.clearOutput();
			}, { separator: "top" });
		}
	}

	clearOutput() {
		let lastValue = this.outputArea.value;
		this.outputArea.value = "";
		return lastValue;
	}

	addOutputLine(line) {
		this.outputArea.value += line + NL;
		this.outputArea.scrollTop = this.outputArea.scrollHeight;
	}

	setRunning(flag) {
		this.isRunning = flag;

		setVisibility(this.workIndicator, flag);
		this.runButton.disabled = flag;

		if (flag) {
			this.runButton.classList.remove("cmd-button");
			this.runButton.classList.add("cmd-button-disabled");
		} else {
			this.runButton.classList.remove("cmd-button-disabled");
			this.runButton.classList.add("cmd-button");
		}
	}
}

/**
 */
export class WorkViewHeaderMenu {

	containerElem = null;
	menuElem = null;
	isVisible = false;

	constructor(containerElem) {
		this.containerElem = containerElem;
		this.menuElem = containerElem.children[0];

		window.addEventListener("click", (event) => {
			this.onAnyWindowClick(event)
		});
	}

	hasItems() {
		return this.menuElem?.children.length > 0;
	}

	close() {
		this.isVisible = false;
		setDisplay(this.menuElem, this.isVisible);
	}

	addItem(text, cb, props = {}) {
		let item = document.createElement("a");
		item.id = props?.id;
		item.href = "javascript:void(0)";
		item.innerHTML = text;

		if (props?.separator) {
			let clazz = props.separator === "top" ? "menu-separator-top" : "menu-separator-bottom";
			item.classList.add(clazz);
		}
		item.onclick = (evt) => cb(evt);

		this.menuElem.appendChild(item);
	}

	toggleVisibility() {
		if (this.hasItems()) {
			this.isVisible = !this.isVisible
			setDisplay(this.menuElem, this.isVisible);
		}
	}

	onAnyWindowClick(event) {
		this.close();
	}
}

/**
 */
export class ModalDialog {
	containerElem;
	header;
	title;
	closeIcon;
	viewArea;
	commandArea;

	constructor(containerElem) {
		this.containerElem = containerElem;
		this.header = getChildOf(containerElem, "modal.dialog.header");
		this.title = getChildOf(this.header, "dialog.title");
		this.viewArea = getChildOf(containerElem, "modal.dialog.view.area");
		this.commandArea = getChildOf(containerElem, "modal.dialog.command.area");
		this.closeIcon = getChildOf(containerElem, "close.icon");

		this.closeIcon.addEventListener("click", () => {
			this.close();
		});
	}

	//called from viewManager on open request
	setViewHtml(html) {
		this.viewArea.innerHTML = html;
		return this;
	}

	close() {
		setDisplay(this.containerElem, false);
		this.viewArea.innerHTML = "";
	}

	setTitle(title) {
		this.title.innerHTML = title;
		return this;
	}

	setAction(id, action) {
		getChildOf(this.containerElem, id).onclick = action;
		return this;
	}

	open() {
		setDisplay(this.containerElem, true);
		return this;
	}

	hideArea(area) {
		setDisplay(area, false);
		return this;
	}
}

/**
 */
export class StandardDialog {
	dialogElem;
	contentArea;
	commandArea;
	title;
	closeIcon;
	pbOk;
	pbCancel;
	tfInput;

	constructor() {
		this.dialogElem = document.getElementById('standardDialog');
		this.contentArea = getChildOf(this.dialogElem, "standard.dialog.content.area");
		this.commandArea = getChildOf(this.dialogElem, "standard.dialog.command.area");
		this.title = getChildOf(this.dialogElem, "standard.dialog.title");
		this.closeIcon = getChildOf(this.dialogElem, "standard.dialog.close.icon");
		this.pbOk = getChildOf(this.dialogElem, "pb.standard.dialog.ok");
		this.pbCancel = getChildOf(this.dialogElem, "pb.standard.dialog.cancel");
		
	}

	openConfirmation(text, cb) {
		this.setupFor("confirm", text, null, cb);
		this.dialogElem.showModal();
	}

	openInput(text, value, cb) {
		this.setupFor("input", text, value, cb);
		this.dialogElem.showModal();
		this.tfInput.focus();
		this.tfInput.select();
	}


	setupFor(type, text, value, cb) {

		this.pbOk.onclick = (evt) => {
			evt.stopImmediatePropagation();
			this.dialogElem.close();
			cb(type === "input" ? this.tfInput.value : true);
		};

		this.closeIcon.onclick = (evt) => {
			evt.stopImmediatePropagation();
			this.dialogElem.close();
			cb(null);
		};
		this.pbCancel.onclick = (evt) => {
			evt.stopImmediatePropagation();
			this.dialogElem.close();
			cb(null);
		};

		if (type === "confirm") {
			this.setupForConfirm(text);
		} else if (type === "input") {
			this.setupForInput(text, value);
		}
	}

	setupForConfirm(text){
		this.pbOk.innerHTML = "Yes";
		this.pbCancel.innerHTML = "No";

		if (typeof text === 'string' || text instanceof String) {
			this.title.innerHTML = "";
			this.contentArea.innerHTML = `<p>${text}</p>`;
		} else {
			this.title.innerHTML = text.title ? text.title : "";
			this.contentArea.innerHTML = `<p>${text?.message}</p>`;
		}
	}

	setupForInput(text, value){
		this.pbOk.innerHTML = "Ok";
		this.pbCancel.innerHTML = "Cancel";

		if(!value){value=""};

		if (typeof text === 'string' || text instanceof String) {
			this.title.innerHTML = "";
			this.contentArea.innerHTML = `<p class="std-dlg-input">${text}</p> 
			<input type="text" id="tf.standard.dialog.input" class="standard-dialog-textfield" value=${value}>`;
		} else {
			this.title.innerHTML = text.title ? text.title : "";
			this.contentArea.innerHTML = `<p class="std-dlg-input">${text?.message}</p>
			<input type="text" id="tf.standard.dialog.input" class="standard-dialog-textfield" value=${value}>`;
		}

		this.tfInput = getChildOf(this.dialogElem, "tf.standard.dialog.input");
	}

}


