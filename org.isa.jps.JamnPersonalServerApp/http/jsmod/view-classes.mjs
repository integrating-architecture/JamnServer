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

/**
 * A class to centralize font icon usage.
 */
export class IconElement {
	elem = null;
	type = "";
	shapes = ["", ""];

	constructor(elem, type, shapes) {
		this.elem = elem;
		this.type = type;
		this.shapes = shapes;
	}

	init(cb = (obj) => { }) {
		cb(this);
		return this;
	}

	toggle() {
		this.elem.classList.toggle(this.shapes[0]);
		this.elem.classList.toggle(this.shapes[1]);
		return this;
	}

	and(cb) {
		cb(this.elem);
	}

	static newIcon(name, elem, type="bi") {
		return new IconElement(elem, type, IconElement.#Icons[type][name]).init(IconElement.#Icons[type].defaultInit);
	}

	static iconDef(name, type="bi"){
		return IconElement.#Icons[type][name];
	}

	static #Icons = {
		bi : {
			defaultInit : (iconObj) => {
				//Bootstrap font icons
				iconObj.elem.classList.add(iconObj.type ? iconObj.type : "bi");
				iconObj.elem.classList.add(iconObj.shapes[0]);
			},
			close : ["bi-x-lg", ""],
			pin : ["bi-pin", "bi-pin-angle"],
			collapse : ["bi-chevron-bar-contract", "bi-chevron-bar-expand"],
			dotmenu : ["bi-three-dots-vertical", ""],
			menu : ["bi-list", ""],
			login :  ["bi-person", "bi-person-check"],
			github :  ["bi-github", ""],
			system :  ["bi-laptop", ""],
			command :  ["bi-command", ""],
			tools :  ["bi-tools", ""],
			user :  ["bi-person", ""],
			password :  ["bi-key", ""],
			loginAction : ["bi-box-arrow-in-right", ""],
			tableSort :  ["bi-arrow-down", "bi-arrow-up"]
		}
	}
}

/**
 * Work View base class.
 */
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
	menuIcon = null;

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

	initialize() {
		//to be overwritten
		//only called when isInitialized=false
		this.viewElement = document.getElementById(this.id);
		this.viewTitle = this.getElement("view.title");
		this.viewWorkarea = this.getElement("work.view.workarea");

		this.closeIcon = IconElement.newIcon("close", this.getElement("close.icon"));
		this.pinIcon = IconElement.newIcon("pin", this.getElement("pin.icon"));
		this.collapseIcon = IconElement.newIcon("collapse", this.getElement("collapse.icon"));
		this.menuIcon = IconElement.newIcon("dotmenu", this.getElement("menu.icon"));

		let elem = this.getElement("header.menu");
		if (elem && this.closeIcon.elem) {
			this.headerMenu = new WorkViewHeaderMenu(elem);

			this.headerMenu.addItem("Close", (evt) => {
				this.closeIcon.elem.click();
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

		this.pinIcon.toggle().and((icon) => {
			icon.title = this.isPinned ? "Unpin view" : "Pin to keep view";

			if (this.isPinned) {
				this.closeIcon.elem.style["pointer-events"] = "none";
			} else {
				this.closeIcon.elem.style["pointer-events"] = "all";
			}
		});

		return this.isPinned;
	}

	toggleCollapsed() {
		this.isCollapsed = !this.isCollapsed;

		this.collapseIcon.toggle().and((icon) => {
			icon.title = this.isCollapsed ? "Expand view" : "Collapse  view";

			setDisplay(this.viewWorkarea, !this.isCollapsed);
		});

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
	
		this.closeIcon = IconElement.newIcon("close", getChildOf(containerElem, "modal.dialog.close.icon"));
		this.closeIcon.elem.addEventListener("click", () => {
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

	setElement(id, cb) {
		cb(getChildOf(this.containerElem, id));
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
		this.pbOk = getChildOf(this.dialogElem, "pb.standard.dialog.ok");
		this.pbCancel = getChildOf(this.dialogElem, "pb.standard.dialog.cancel");

		this.closeIcon = IconElement.newIcon("close", getChildOf(this.dialogElem, "standard.dialog.close.icon"));
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

		this.closeIcon.elem.onclick = (evt) => {
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

	setupForConfirm(text) {
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

	setupForInput(text, value) {
		this.pbOk.innerHTML = "Ok";
		this.pbCancel.innerHTML = "Cancel";

		if (!value) { value = "" };

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

/**
 */
export class WorkViewTable {
	tableElem;
	tableBody;
	tableData = null;
	ascOrder = true;
	sortIcon;

	constructor(tableElem) {
		this.tableElem = tableElem;
		this.tableBody = this.tableElem.querySelector('tbody');

		let icon = this.getHeader(0).getElementsByTagName("i")[0];
		this.sortIcon = IconElement.newIcon("tableSort", icon);
	}

	getHeader(idx) {
		return this.tableElem.getElementsByTagName("th")[idx];
	}

	setData(tableData) {
		this.clearData();
		this.tableData = tableData;

		this.tableData.rows.forEach((rowData, rowKey) => {
			let row = document.createElement("tr");
			row.className = "wkv";

			rowData.forEach((colVal, colKey) => {
				let col = document.createElement("td");
				col.className = "wkv";
				col.innerHTML = colVal;
				col.value = colKey;
				col.onclick = (evt) => {
					this.tableData.cellClick(rowKey, colKey, evt);
				}
				row.appendChild(col);
			});

			this.tableBody.appendChild(row);
		});
	}

	clearData() {
		this.tableData = null;
		this.tableBody.replaceChildren();
	}

	sortByColumn(colIdx) {
		this.ascOrder = !this.ascOrder;
		let rows = Array.from(this.tableBody.querySelectorAll('tr'));

		rows.sort((rowA, rowB) => {
			let cellA = rowA.querySelectorAll('td')[colIdx].textContent.trim();
			let cellB = rowB.querySelectorAll('td')[colIdx].textContent.trim();

			return this.ascOrder ? cellA.localeCompare(cellB) : cellB.localeCompare(cellA);
		});

		this.tableBody.replaceChildren();
		this.tableBody.append(...rows);
	}

	toggleColSort(colIdx) {
		this.sortIcon.toggle();
	}

	filterRows(colIdx, filterText) {
		let rows = Array.from(this.tableBody.querySelectorAll('tr'));
		let filter = filterText.toLowerCase();

		rows.forEach((row) => {
			let cellVal = row.querySelectorAll('td')[colIdx].textContent;
			row.style.display = cellVal.toLowerCase().indexOf(filter) < 0 ? "none" : "";
		});
	}
}

/**
 * Table data represented as 
 * - map of rows (key:row)
 *  - each row a map of columns (key:column)
 */
export class TableData {
	rows;
	cellClick;

	constructor() {
		this.rows = new Map();
		this.cellClick = (rowKey, colKey, evt) => { };
	}

	addRow(key, columns) {
		this.rows.set(key, columns);
	}

}

