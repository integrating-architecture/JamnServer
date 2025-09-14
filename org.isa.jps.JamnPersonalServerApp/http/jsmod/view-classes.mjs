/* Authored by iqbserve.de */

import { getChildOf, setVisibility, setDisplay, typeUtil, fileUtil, fetchPlainText, mergeArrayInto } from '../jsmod/tools.mjs';
import { ViewSource } from '../jsmod/data-classes.mjs';
import { WorkbenchInterface as WbApp } from '../jsmod/workbench.mjs';
import * as Icons from '../jsmod/icons.mjs';

const getOrDefault = (obj, name, defaultVal = null) => {
	if (!obj) { return defaultVal }
	return obj[name] ? obj[name] : defaultVal;
};

/**
 * Get a view html file from the server
 */
function getViewHtml(viewSrc, cb) {
	if (viewSrc.isEmpty()) {
		//load the html from server
		fetchPlainText(viewSrc.getFile()).then((html) => {
			viewSrc.setHtml(html);
			cb(viewSrc.getHtml());
		});
	} else {
		cb(viewSrc.getHtml());
	}
}

/**
 * Create a standalone view dom element
 */
function createViewElementFor(view, html) {
	let template = document.createElement("template");
	template.innerHTML = html;
	view.viewElement = template.content.firstElementChild;
	view.viewElement.id = view.id;
}

/**
 * Shortcuts for addEventListener
 */

export function onClicked(elem, action) {
	elem.addEventListener("click", action);
}

export function onDblClicked(elem, action) {
	elem.addEventListener("dblclick", action);
}

export function onInput(elem, action) {
	elem.addEventListener("input", action);
}

export function onKeyup(elem, action) {
	elem.addEventListener("keyup", action);
}

/**
 * A basic view class. 
 */
export class AbstractView {

	id = "";
	viewSource = new ViewSource("");
	viewElement = null;
	isInitialized = false;

	constructor(id, file = null) {
		this.id = id;
		this.viewSource = new ViewSource(file);

		this.isInitialized = false;
	}

	/**
	 * Get and lazy create the view dom element.
	 */
	getViewElement(cb = (elem) => { }) {
		if (!this.isInitialized || this.viewElement == null) {
			getViewHtml(this.viewSource, (html) => {
				createViewElementFor(this, html);
				this.initialize();
				cb(this.viewElement);
			});
		} else {
			cb(this.viewElement);
		}
	}

	/**
	 * The method is called by getViewElement.
	 */
	initialize() {
		//to be overwritten
	}

	setVisible(flag) {
		setVisibility(this.viewElement, flag);
	}

	setDisplay(elem, flag) {
		setDisplay(elem, flag);
	}

	getElement(id) {
		return getChildOf(this.viewElement, id);
	}

	writeDataToView() {
		//to be overwritten
	}

	readDataFromView() {
		//to be overwritten
	}

	setTitle(title) {
		//to be overwritten
	}
}

/**
 * Work View base class.
 */
export class WorkView extends AbstractView {
	viewManager = null;

	viewHeader;
	viewBody;
	viewWorkarea;
	sidePanel;

	bodyInitialDisplay;

	state = {
		isRunning: false,
		isOpen: false,
		isPinned: false,
		isCollapsed: false
	}

	constructor(id, file) {
		super(id, file);

		this.state.isRunning = false;
		this.state.isOpen = false;
	}

	initialize() {
		//to be overwritten
		//called from getViewElement

		this.viewBody = this.getElement("work.view.body");
		this.viewWorkarea = this.getElement("work.view.workarea");
		this.bodyInitialDisplay = this.viewBody.style.display;

		this.viewHeader = new WorkViewHeader(this, this.state);
		this.viewHeader.rightIconBar((bar) => {
			bar.addIcon({ id: "close.icon", title: "Close view" }, Icons.close(), (evt) => {
				this.viewManager.onViewAction(evt, "close");
			});
			bar.addIcon({ id: "pin.icon", title: "Pin to keep view" }, Icons.pin(), (evt) => {
				this.togglePinned(evt);
			});
			bar.addIcon({ id: "collapse.icon", title: "Collapse view" }, Icons.collapse(), (evt) => {
				this.toggleCollapsed(evt);
			});
		});

		this.viewHeader.menu((menu) => {
			menu.addItem("Close", (evt) => {
				this.viewManager.onViewAction(evt, "close");
			}, { separator: "bottom" });

			if (this.viewManager) {
				menu.addItem("Move up", (evt) => {
					this.viewManager.moveView(this, "up");
				});
				menu.addItem("Move down", (evt) => {
					this.viewManager.moveView(this, "down");
				});
				menu.addItem("Move to ...", (evt) => {
					this.viewManager.promptUserInput({ title: "", message: "Please enter your desired position number:" }, "1",
						(value) => value ? this.viewManager.moveView(this, value) : null
					);
				});
			}
		});
	}

	open(data = null) {
		this.viewHeader.menu().close();
		this.state.isOpen = true;
	}

	setVisible(flag) {
		setVisibility(this.viewElement, flag);
	}

	close() {
		if (this.isInitialized) {
			this.state.isOpen = false;
			this.viewHeader.menu().close();
		}
		return this.isCloseable();
	}

	isCloseable(ctxObj = null) {
		return !(this.state.isRunning || this.state.isPinned);
	}

	setRunning(flag) {
		this.state.isRunning = flag;
		this.viewHeader.showRunning(flag);
	}

	setTitle(title) {
		this.viewHeader.setTitle(title);
	}

	getElement(id) {
		return getChildOf(this.viewElement, id);
	}

	onInstallation(installKey, installData, viewManager) {
		this.viewManager = viewManager;
		if (installKey && this.id.length == 0) {
			this.id = installKey;
		}
	}

	installSidePanel(sidePanelViewElem, workareaElem = this.viewWorkarea) {
		this.sidePanel = new WorkViewSidepanel(this, workareaElem);
		this.sidePanel.setViewComp(sidePanelViewElem);

		this.viewHeader.rightIconBar((bar) => {
			bar.addIcon({ id: "sidepanel.icon", title: "Show/Hide Sidepanel" }, Icons.wkvSidePanel(), (evt) => {
				this.toggleSidePanel(evt);
			});
		});

		return this.sidePanel;
	}

	toggleSidePanel(evt = null) {
		this.sidePanel.toggle();
		this.viewHeader.icons["sidepanel.icon"].toggle((icon) => {
			icon.title = this.sidePanel.isOpen() ? "Hide Sidepanel" : "Show Sidepanel";
		});
	}

	togglePinned(evt = null) {
		this.state.isPinned = !this.state.isPinned;

		this.viewHeader.icons["pin.icon"].toggle((icon) => {
			icon.title = this.state.isPinned ? "Unpin view" : "Pin to keep view";
			this.viewHeader.icons["close.icon"].setEnabled(!this.state.isPinned);
		});

		return this.state.isPinned;
	}

	toggleCollapsed(evt = null) {
		this.state.isCollapsed = !this.state.isCollapsed;

		this.viewHeader.icons["collapse.icon"].toggle((icon) => {
			icon.title = this.state.isCollapsed ? "Expand view" : "Collapse  view";
			let displayVal = !this.state.isCollapsed ? this.bodyInitialDisplay : "none";
			if (displayVal == "none" && this.sidePanel?.isOpen()) {
				this.toggleSidePanel();
			}
			setDisplay(this.viewBody, displayVal);
		});
		this.viewHeader.icons["sidepanel.icon"]?.setEnabled(!this.state.isCollapsed);
		return this.state.isCollapsed;
	}

	statusLineInfo(info) {
		WbApp.statusLineInfo(info);
	}

	copyToClipboard(text) {
		if (!this.state.isRunning && (text && text.length > 0)) {
			navigator.clipboard.writeText(text);
		}
	}

	saveToFile(fileName, text) {
		if (!this.state.isRunning && text.length > 0) {
			fileUtil.saveToFileClassic(fileName, text);
		}
	}

}

/**
 */
export class WorkViewHeader {
	view;
	viewState;

	icons = {};
	headerMenu;
	iconBarLeft;
	iconBarRight;
	progressBar;
	title;

	constructor(view, viewState) {
		this.view = view;
		this.viewState = viewState;
		this.#initialize();
	}

	#initialize() {
		this.title = this.#getElement("view.title");
		this.headerMenu = new WorkViewHeaderMenu(this.#getElement("header.menu"));

		this.iconBarLeft = new WorkViewHeaderIconBar(this.#getElement("wkv.header.iconbar.left"), this.icons);
		this.iconBarLeft.addIcon({ id: "menu.icon", title: "View Menu" }, Icons.dotmenu(), (evt) => {
			this.#toggleHeaderMenu(evt);
		});
		this.iconBarRight = new WorkViewHeaderIconBar(this.#getElement("wkv.header.iconbar.right"), this.icons);
		this.progressBar = this.#getElement("wkv.header.progressbar");
	}

	#getElement(id) {
		return this.view.getElement(id);
	}

	#toggleHeaderMenu(evt = null) {
		if (evt) { evt.stopImmediatePropagation(); }
		if (!this.viewState.isCollapsed) {
			this.headerMenu.toggleVisibility(evt);
		}
	}

	leftIconBar(configCb = null) {
		if (configCb) {
			configCb(this.iconBarLeft);
		}
		return this.iconBarLeft;
	}

	rightIconBar(configCb = null) {
		if (configCb) {
			configCb(this.iconBarRight);
		}
		return this.iconBarRight;
	}

	menu(configCb = null) {
		if (configCb) {
			configCb(this.headerMenu);
		}
		return this.headerMenu;
	}

	showRunning(flag = null) {
		let classList = this.progressBar.firstElementChild.classList;
		let clazz = "progress-showWorking";
		classList.toggle(clazz);
		if (!this.viewState.isRunning && classList.contains(clazz)) {
			console.warn("isRunning flag mismatch");
		}
	}

	setTitle(text) {
		this.title.innerHTML = text;
	}
}

/**
 */
export class WorkViewSidepanel {
	view;
	splitHandler;
	splitterElem;
	sidePanelElem;
	workareaElem;
	viewCompElem;

	constructor(view, workareaElem) {
		this.view = view;
		this.workareaElem = workareaElem;
		this.#initialize();
	}

	#initialize() {

		this.splitterElem = this.view.getElement("work.view.sidepanel.splitter");
		this.sidePanelElem = this.view.getElement("work.view.sidepanel");

		if (this.splitterElem && this.sidePanelElem) {
			this.splitHandler = new SplitBarHandler(
				this.splitterElem,
				this.workareaElem,
				this.sidePanelElem
			);
			this.setWidth("100px");
		}
	}

	#isClosed() {
		let val = this.splitterElem.style.display;
		return (!val || val == "none");
	}

	isOpen() {
		return !this.#isClosed();
	}

	toggle() {
		if (this.#isClosed()) {
			this.open();
		} else {
			this.close();
		}
		return this;
	}

	open() {
		setDisplay(this.splitterElem, true);
		setDisplay(this.sidePanelElem, true);
		return this;
	}

	close() {
		setDisplay(this.splitterElem, false);
		setDisplay(this.sidePanelElem, false);
		return this;
	}

	setViewComp(compElem) {
		this.viewCompElem = compElem;
		this.sidePanelElem.append(this.viewCompElem);
		return this;
	}

	setWidth(width) {
		this.sidePanelElem.style.width = width;
		return this;
	}
}

/**
 */
export class SplitBarHandler {

	splitter;
	compBefore;
	compAfter;
	orientation = "v";
	moveSplitter = false;

	clickPoint;

	barrierActionBefore = (handler, value) => { };
	barrierActionAfter = (handler, value) => { };

	constructor(splitter, compbefore, compafter) {
		this.splitter = splitter;
		this.compBefore = compbefore;
		this.compAfter = compafter;

		this.splitter.onmousedown = (evt) => {
			this.#onDragStart(evt);
		}
	}

	#onDragStart(evt) {

		this.splitter.classList.toggle("vsplitter-working");

		this.clickPoint = {
			evt,
			offsetLeft: this.splitter.offsetLeft,
			offsetTop: this.splitter.offsetTop,
			beforeWidth: this.compBefore.offsetWidth,
			afterWidth: this.compAfter.offsetWidth
		};

		//avoid cursor flicker
		let cursor = window.getComputedStyle(this.splitter)["cursor"];
		this.compBefore.style.cursor = cursor;
		this.compAfter.style.cursor = cursor;

		document.onmousemove = (evt) => {
			this.#doDrag(evt);
		};

		document.onmouseup = () => {
			document.onmousemove = document.onmouseup = null;
			this.compBefore.style.cursor = "default";
			this.compAfter.style.cursor = "default";
			this.splitter.classList.toggle("vsplitter-working");
		}
	}

	#doDrag(evt) {
		let delta = {
			x: evt.clientX - this.clickPoint.evt.clientX,
			y: evt.clientY - this.clickPoint.evt.clientY
		};

		if (this.orientation === "v") {
			this.#doVDrag(delta, evt);
		}
	}

	#doVDrag(delta, evt) {
		delta.x = Math.min(Math.max(delta.x, -this.clickPoint.beforeWidth),
			this.clickPoint.afterWidth);

		let val = this.clickPoint.offsetLeft + delta.x;
		if (this.barrierActionBefore(this, val)) { return; }

		if (this.moveSplitter) {
			this.splitter.style.left = val + "px";
		}
		this.compBefore.style.width = (this.clickPoint.beforeWidth + delta.x) + "px";
		this.compAfter.style.width = (this.clickPoint.afterWidth - delta.x) + "px";
	}

	stop() {
		document.dispatchEvent(new Event("mouseup", { bubbles: true, cancelable: true }));
	}
}

/**
 */
export class WorkViewHeaderMenu {

	containerElem = null;
	menuElem = null;
	isVisible = false;

	constructor(containerElem) {
		this.menuElem = containerElem;

		onClicked(window, (event) => {
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
		item.href = "view: " + text;
		item.innerHTML = text;

		if (props?.separator) {
			let clazz = props.separator === "top" ? "menu-separator-top" : "menu-separator-bottom";
			item.classList.add(clazz);
		}

		onClicked(item, (evt) => {
			//cause <a> links are used as menu items 
			evt.preventDefault();
			cb(evt);
		});

		if (props?.pos) {
			this.menuElem.insertAdjacentElement(props.pos, item);
		} else {
			this.menuElem.appendChild(item);
		}
	}

	toggleVisibility(evt = null) {
		if (this.hasItems()) {
			let trigger = evt.currentTarget;
			this.menuElem.style.left = trigger.offsetLeft + trigger.offsetWidth + 10 + "px";
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
export class WorkViewHeaderIconBar {
	builder;
	iconBarComp;
	items;

	constructor(iconBarElem, items = {}) {
		this.items = items;
		this.builder = new ViewBuilder();
		this.iconBarComp = new ViewComp(this.builder, iconBarElem, null);
	}

	addIcon(props, icon, action) {
		this.iconBarComp.addActionIcon({ "iconName": icon, "title": props.title }, (target) => {
			onClicked(target.icon, (evt) => { action(evt); });
			this.items[props.id] = target.iconElement;
		});
	}

	getIconElement(id) {
		return this.items[id];
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

		this.closeIcon = Icons.close(getChildOf(containerElem, "modal.dialog.close.icon")).init((icon) => {
			onClicked(icon.elem, () => { this.close(); });
		});
	}

	//called from viewManager 
	setDialogViewElement(viewElement) {
		this.viewArea.append(viewElement);
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
		onClicked(getChildOf(this.containerElem, id), action);
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
		this.dialogElem = document.getElementById("standardDialog");
		this.contentArea = getChildOf(this.dialogElem, "standard.dialog.content.area");
		this.commandArea = getChildOf(this.dialogElem, "standard.dialog.command.area");
		this.title = getChildOf(this.dialogElem, "standard.dialog.title");
		this.pbOk = getChildOf(this.dialogElem, "pb.standard.dialog.ok");
		this.pbCancel = getChildOf(this.dialogElem, "pb.standard.dialog.cancel");

		this.closeIcon = Icons.close(getChildOf(this.dialogElem, "standard.dialog.close.icon"));
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

		onClicked(this.pbOk, (evt) => {
			this.dialogElem.close();
			cb(type === "input" ? this.tfInput.value : true);
		});

		onClicked(this.pbCancel, (evt) => {
			this.dialogElem.close();
			cb(null);
		});

		onClicked(this.closeIcon.elem, (evt) => {
			this.dialogElem.close();
			cb(null);
		});

		if (type === "confirm") {
			this.setupForConfirm(text);
		} else if (type === "input") {
			this.setupForInput(text, value);
		}
	}

	setupForConfirm(text) {
		this.pbOk.innerHTML = "Yes";
		this.pbCancel.innerHTML = "No";
		let title = "Confirmation required";

		if (typeof text === 'string' || text instanceof String) {
			this.title.innerHTML = title;
			this.contentArea.innerHTML = `<p>${text}</p>`;
		} else {
			this.title.innerHTML = text.title ? text.title : title;
			this.contentArea.innerHTML = `<p>${text?.message}</p>`;
		}
	}

	setupForInput(text, value) {
		this.pbOk.innerHTML = "Ok";
		this.pbCancel.innerHTML = "Cancel";
		let title = "Input";

		if (!value) { value = "" };

		if (typeof text === 'string' || text instanceof String) {
			this.title.innerHTML = title;
			this.contentArea.innerHTML = `<p class="std-dlg-input">${text}</p> 
			<input type="text" id="tf.standard.dialog.input" class="standard-dialog-textfield" value=${value}>`;
		} else {
			this.title.innerHTML = text.title ? text.title : title;
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
	ascOrder = false;
	sortIcon;

	constructor(tableElem) {
		this.tableElem = tableElem;
		this.tableBody = this.tableElem.querySelector('tbody');

		let iconElem = this.getHeader(0).getElementsByTagName("i")[0];
		this.sortIcon = Icons.tableSort(iconElem);
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
				onClicked(col, (evt) => { this.tableData.cellClick(rowKey, colKey, evt); });
				onDblClicked(col, (evt) => { this.tableData.cellDblClick(rowKey, colKey, evt); });
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

	newCellInputField(props = { clazz: "wkv-tblcell-edit-tf", booleanValue: null, datalist: [] }) {
		let comp = document.createElement('span');
		let ctrl = document.createElement('input');

		ctrl.comp = comp;
		ctrl.type = "text";
		ctrl.classList.add(props.clazz ? props.clazz : "wkv-tblcell-edit-tf");
		comp.append(ctrl);

		if (props?.booleanValue != null) {
			ctrl.type = "checkbox";
			ctrl.checked = props.booleanValue;
			ctrl.style.width = "20px";
			onClicked(ctrl, (evt) => { ctrl.value = typeUtil.stringFromBoolean(ctrl.checked) });
		} else if (props.datalist?.length > 0) {
			let item = null;
			let dataElem = document.createElement("datalist");
			dataElem.id = Math.random().toString(32).slice(5);
			props.datalist.forEach(entry => {
				item = document.createElement("option");
				item.value = entry;
				dataElem.append(item);
			});
			ctrl.setAttribute("list", dataElem.id);
			comp.append(dataElem);
		}
		return ctrl;
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
	cellDblClick;

	constructor() {
		this.rows = new Map();
		this.cellClick = (rowKey, colKey, evt) => { };
		this.cellDblClick = (rowKey, colKey, evt) => { };
	}

	addRow(key, columns) {
		this.rows.set(key, columns);
	}
}

/**
 */
export class DataList {
	data;
	ctrl;
	listElem;

	constructor(ctrl) {
		this.ctrl = ctrl;
		this.listElem = document.createElement("datalist");
		this.listElem.id = "data." + ctrl.id;
		this.ctrl.setAttribute("list", this.listElem.id);
	}

	#newOption(item) {
		let option = document.createElement("option");
		option.id = item.id ? item.id : item;
		option.value = item.value ? item.value : option.id;
		return option;
	}

	setOptions(optionValues) {
		let option = null;
		optionValues.forEach(item => {
			option = this.#newOption(item);
			this.listElem.append(option);
		});
	}

	removeOption(id) {
		ViewBuilder.removeChildFrom(this.listElem, id);
	}

	addOption(item) {
		let id = item.id ? item.id : item;
		let option = ViewBuilder.getChildFrom(this.listElem, id);
		if (option === null) {
			option = this.#newOption(item);
			this.listElem.prepend(option);
		}
	}

	addDataItem(key, item) {
		this.addOption(key);
		this.data[key] = item;
	}

	removeDataItem(key) {
		this.removeOption(key);
		delete this.data[key];
	}

}

/*********************************************************************************
 * UI BUILDER CLASSES
 *********************************************************************************/
/**
 */
class DefaultCompProps {

	static makeACopyOf(source) {
		let newProps = { ...source };
		newProps.clazzes = mergeArrayInto(newProps.clazzes, source.clazzes);
		newProps.attribProps = source.attribProps ? { ...source.attribProps } : {};
		newProps.styleProps = source.styleProps ? { ...source.styleProps } : {};
		delete newProps['clazzFilter'];
		return newProps;
	}

	blankComp = { elemType: "div", clazzes: [], attribProps: {}, styleProps: {} };
	comp = { elemType: "div", clazzes: ["wkv-comp", "row-comp"], attribProps: {}, styleProps: {} };
	colComp = { elemType: "div", clazzes: ["wkv-comp", "col-comp"], attribProps: {}, styleProps: {} };
	rowComp = { elemType: "div", clazzes: ["wkv-comp", "row-comp"], attribProps: {}, styleProps: {} };

	fieldset = { clazzes: ["wkv-compset"], attribProps: {}, styleProps: {} };
	titledFieldset = { clazzes: ["wkv-compset", "wkv-compset-border"], attribProps: {}, styleProps: {} };
	container = { elemType: "span", clazzes: ["wkv-container"], attribProps: {}, styleProps: {} };
	rowContainer = { elemType: "span", clazzes: ["wkv-container", "row-container"], attribProps: {}, styleProps: {} };
	colContainer = { elemType: "span", clazzes: ["wkv-container", "col-container"], attribProps: {}, styleProps: {} };

	label = { clazzes: ["wkv-label-ctrl"], attribProps: {}, styleProps: {} };
	link = { clazzes: ["wkv-link-ctrl"], attribProps: {}, styleProps: {} };
	list = { elemType: "ul", clazzes: ["wkv-list-ctrl"], attribProps: {}, styleProps: {} };
	actionIcon = { clazzes: ["wkv-action-icon"], attribProps: {}, styleProps: {} };
	button = { clazzes: ["wkv-button-ctrl"], attribProps: {}, styleProps: {} };
	textField = { clazzes: ["wkv-value-ctrl"], attribProps: {}, styleProps: {} };
	textArea = { clazzes: ["wkv-textarea-ctrl"], attribProps: {}, styleProps: {} };
	hr = { clazzes: ["solid"], attribProps: {}, styleProps: {} };

	inputReadOnly = { clazzes: ["input-readonly"], attribProps: {}, styleProps: {} };
	textareaReadOnly = { clazzes: ["textarea-readonly"], attribProps: {}, styleProps: {} };

	get(id) {
		return this[id];
	}

	getClassesFor(id) {
		return this[id]?.clazzes;
	}
	getStylesFor(id) {
		return this[id]?.styleProps;
	}
	getAttributesFor(id) {
		return this[id]?.attribProps;
	}
}

export function makeACopyOfCompProps(source) {
	return DefaultCompProps.makeACopyOf(source);
}

/**
 * <pre>
 * An experimental factory/builder to programmatically create UI components and views.
 * 
 * A ViewBuilder instance is the starting point.
 * It serves as a dataobject and as a static function provider.
 * 
 * The actual builder objects are instances of ViewComp()
 *  - vc = builder.newViewComp()
 * 
 * ViewComps are the building blocks/container for views
 * 
 * ViewComp objects offer element builder methods for e.g. label, field, button etc.
 * in a Chaining-Way and use chained closures to build nested structures.
 *  - vc.addLabelButton({ text: "Command:" })
 *      .attrib("title": "Run")
 *      .style({ "align-items": "flex-start", "text-align": "center" })
 *      ...
 *      .addColContainer( {type: "span"}, (target) => {
 *         target.comp.addXY ...
 *         ... 
 *       })
 *    ...
 * ...
 * 
 * The advantage is 
 * - the JS code structure is similar to the HTML structure
 * - html tags are reflected by functions
 * - attributes and styles are strings 
 * - all elements are directly available without the need to declare variables or perform searches
 * - builders are ad hoc extendable
 * - everything is plain js code 
 * </pre>
 */
export class ViewBuilder {

	static #setterAttributes = [];
	static #valueClearableInputTypes = ["text", "password"];

	//instance variables
	#defaultCompProps = new DefaultCompProps();

	viewCompFactory = {
		newViewComp: (builder, element, props) => {
			return new ViewComp(builder, element, props);
		}
	};

	//all elements with a varid are put to the collection
	//elem = elementCollection.<varid>
	elementCollection = {};

	//collection for any objects
	objectCollection = {};
	//extendable interface to collect any objects 
	objectsToCollect = ["data-bind"];
	objectCollector = (obj, collection, props = null) => {
		if (!collection.bindings) { collection["bindings"] = {}; };
		this.objectsToCollect.forEach((name) => {
			let value;
			if (obj.hasOwnProperty(name)) {
				value = obj[name];
				if (name == "data-bind") {
					collection.bindings[value] = obj;
				} else if (!collection.hasOwnProperty(value)) {
					collection[value] = obj;
				}
			}
		});
	};

	static clearControl(ctrl) {

		let tagName = ctrl.tagName.toLowerCase();
		if (tagName === "input") {
			if (ViewBuilder.#valueClearableInputTypes.includes(ctrl.type)) {
				ctrl.value = "";
			}
		} else if (tagName === "textarea") {
			ctrl.value = "";
		}
	}

	static createDomElementFrom(html, tagName = "template") {
		let template = document.createElement(tagName);
		if (html) {
			template.innerHTML = html;
		}
		if (tagName.toLowerCase() == "template") {
			return template.content.firstElementChild;
		}
		return template;
	}

	static removeChildFrom(parent, id) {
		let node = ViewBuilder.getChildFrom(parent, id);
		if (node) { parent.removeChild(node); }
	}

	static getChildFrom(parent, id) {
		for (let child of parent.childNodes) {
			if (child.id === id) { return child; }
		}
		return null;
	}

	static reworkId(id) {
		if (!id || id === 'undefined' || id === "") {
			return Math.random().toString(32).slice(5);
		}
		return id;
	}

	static setClassesOf(ctrl, clazzes, defaultClazzes = null) {
		if (typeUtil.isArray(clazzes)) {
			clazzes.forEach(clazz => ctrl.classList.add(clazz));
		} else if (clazzes) {
			ctrl.classList.add(clazzes);
		} else if (defaultClazzes) {
			ViewBuilder.setClassesOf(ctrl, defaultClazzes, null);
		}
	}

	static setStyleOf(ctrl, styleProps) {
		for (const name in styleProps) {
			ctrl.style[name] = styleProps[name];
		}
	}

	static setAttributesOf(ctrl, attributeProps) {
		for (const name in attributeProps) {
			if (ViewBuilder.#setterAttributes.includes(name)) {
				ctrl.setAttribute(name, attributeProps[name]);
			} else {
				ctrl[name] = attributeProps[name];
			}
		}
	}

	static checkAndReworkCompProps(props) {
		//ensure clazzes is an array
		if (!props.clazzes) {
			props.clazzes = [];
		} else if (typeUtil.isString(props.clazzes)) {
			props.clazzes = [props.clazzes];
		}
	}

	setViewCompFactory(factory) {
		this.viewCompFactory = factory;
		return this;
	}

	setElementCollection(obj) {
		this.elementCollection = obj;
		return this;
	}

	setObjectCollection(collection, collector = null) {
		this.objectCollection = collection;
		if (collector) { this.objectCollector = collector; }
		return this;
	}

	forEachElement(cb) {
		let elements = this.elementCollection;
		let names = Object.getOwnPropertyNames(elements);
		names.forEach((name) => {
			let ctrl = elements[name];
			cb(name, ctrl);
		});
	}

	forEachBinding(cb) {
		let bindings = this.objectCollection.bindings;
		let names = Object.getOwnPropertyNames(bindings);
		names.forEach((name) => {
			let ctrl = bindings[name];
			cb(name, ctrl);
		});
	}

	getDataListFor(name) {
		return this.objectCollection[this.elementCollection[name].list.id];
	}

	setCompPropDefaults(cb) {
		cb(this.#defaultCompProps);
		return this;
	}

	getDefaultCompProps() {
		return this.#defaultCompProps;
	}

	newViewComp(props = null) {
		return this.viewCompFactory.newViewComp(this, null, props);
	}

	newViewCompFor(element) {
		return this.viewCompFactory.newViewComp(this, element, null);
	}
}

/**
 * The view component object is the wrapper to code view elements
 * in a chaining builder like style.
 */
export class ViewComp {

	static #directSupportedAttributes = ["html", "innerHTML", "disabled"];
	static #mapDirectAttribute = (name) => {
		if (name === "html") { name = "innerHTML" }
		return name;
	};

	builder;
	elem = null;
	parent = null;
	listener = null;
	bag = [];

	constructor(builder, element, props) {
		this.builder = builder;
		if (element) {
			this.elem = element;
		} else {

			let compType = getOrDefault(props, "compType", "comp");
			let defaultProps = this.#getDefaultCompPropsFor(compType);

			if (!props) {
				props = defaultProps;
			} else {
				this.#mergeClazzesIntoArgumentProps(props, defaultProps);
			}

			let elemType = getOrDefault(props, "elemType", getOrDefault(defaultProps, "elemType", "div"));

			this.elem = document.createElement(elemType);
			this.#setClassesOf(this.elem, props.clazzes, compType);
			this.#applyDefaultStyles(this.elem, compType);
			this.#applyProperties(this.elem, props)
		}
	}

	/**
	 * INTERNAL
	 * private methods used by the ViewComp itself
	 * to realize the actual element builder functions
	 */
	#newViewComp(element, parent = null) {
		let comp = this.builder.newViewCompFor(element);
		comp.parent = parent;
		comp.listener = parent.listener;
		comp.bag = parent.bag;
		return comp;
	}

	#reworkId(id) {
		return ViewBuilder.reworkId(id);
	}

	#setupIconFor(ctrl, props) {
		if (props.iconName) {
			return Icons.newIcon(props.iconName).apply(ctrl);
		}
	}

	#setClassesOf(ctrl, clazzes, defaultId) {
		ViewBuilder.setClassesOf(ctrl, clazzes, this.#getDefaultCompProps().getClassesFor(defaultId));
	}

	#setStyleOf(ctrl, styleProps) {
		ViewBuilder.setStyleOf(ctrl, styleProps);
	}

	#setAttributesOf(ctrl, attribProps) {
		ViewBuilder.setAttributesOf(ctrl, attribProps);
	}

	#applyDefaultStyles(ctrl, typeId) {
		let styles = this.#getDefaultCompProps().getStylesFor(typeId);
		if (styles) {
			this.#setStyleOf(ctrl, styles);
		}
	}

	#applyDirectAttributeProperties(ctrl, props) {
		//comfort method
		//apply the list of direct supported attributes if any in props
		let attributeValues = {};
		ViewComp.#directSupportedAttributes.forEach((name) => {
			if (props.hasOwnProperty(name)) {
				let attributeName = ViewComp.#mapDirectAttribute(name);
				attributeValues[attributeName] = props[name];
			}
		});
		if (Object.keys(attributeValues).length > 0) {
			this.#setAttributesOf(ctrl, attributeValues);
		}
	}

	#applyProperties(ctrl, props) {

		this.#applyDirectAttributeProperties(ctrl, props);

		// apply the dedicated props
		if (props.attribProps) {
			this.#setAttributesOf(ctrl, props.attribProps);
		}
		if (props.styleProps) {
			this.#setStyleOf(ctrl, props.styleProps);
		}
	}

	#registerCtrl(varid, ctrl, props) {
		if (varid && this.builder.elementCollection) {
			this.builder.elementCollection[varid] = ctrl;
		}
		this.#registerObject(ctrl, null, props);
	}

	#registerObject(obj, id, props = null) {
		if (this.builder.objectCollection) {
			if (id) {
				this.builder.objectCollection[id] = obj;
			} else if (this.builder.objectCollector) {
				this.builder.objectCollector(obj, this.builder.objectCollection, props);
			}
		}
	}

	#addCtrlToTarget(props, ctrlElem) {
		if (props.parent) {
			if (props.parent instanceof ViewComp) {
				this.#doCtrlAddingTo(props.parent.elem, ctrlElem, props);
			} else {
				this.#doCtrlAddingTo(props.parent, ctrlElem, props);
			}
		} else {
			this.#doCtrlAddingTo(this.elem, ctrlElem, props);
		}
	}

	#doCtrlAddingTo(compElem, ctrlElem, props) {
		if (props.pos == "top" || props.pos == 0) {
			compElem.prepend(ctrlElem);
		} else {
			compElem.append(ctrlElem);
		}
	}

	#isReadOnly(props) {
		return props.hasOwnProperty('readOnly');
	}

	#newDataList(ctrl, data) {
		let datalist = new DataList(ctrl);
		datalist.setOptions(data);
		this.#registerObject(datalist, datalist.listElem.id);
		return datalist;
	}

	/**
	 */
	#mergeClazzesIntoArgumentProps(argProps, defaultProps) {
		//get all clazzes from source into target
		argProps.clazzes = mergeArrayInto(argProps.clazzes, defaultProps.clazzes);
		if (argProps.clazzFilter) {
			argProps.clazzFilter(argProps.clazzes);
		}
	}

	/**
	 * check/rework args to [props, function]
	 */
	#checkAndReworkArgs(args, defaultProps, argsCb) {
		ViewBuilder.checkAndReworkCompProps(defaultProps);

		if (args.length == 0) {
			args = [defaultProps, null];
		} else if (typeUtil.isFunction(args[0])) {
			//no props - expand to [props, function]
			args.splice(1, 0, args[0]);
			args[0] = defaultProps;
		} else {
			//args correct - merge the default clazzes into current
			ViewBuilder.checkAndReworkCompProps(args[0]);
			this.#mergeClazzesIntoArgumentProps(args[0], defaultProps);
		}

		return argsCb(args.slice(0, 2));
	}

	#getDefaultCompProps() {
		return this.builder.getDefaultCompProps();
	}

	#getDefaultCompPropsFor(type) {
		return this.#getDefaultCompProps().get(type);
	}

	#addCtrlImpl(elemType, typeId, props) {
		let ctrl = document.createElement(elemType);

		this.#setClassesOf(ctrl, props.clazzes, typeId);
		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		this.#addCtrlToTarget(props, ctrl);
		this.#registerCtrl(props.varid, ctrl, props);

		return ctrl;
	}

	#addElementImpl(elemType, props, configCb = null) {
		this.#checkAndReworkArgs([props, configCb], {}, (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl(elemType || "span", "", props);

		if (configCb) {
			this.#callConfig(configCb, { comp: this.#newViewComp(ctrl, this), elem: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	#addContainerImpl(typeId, props, configCb = null) {
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl(getOrDefault(props, "elemType", "span"), typeId, props);

		if (configCb) {
			this.#callConfig(configCb, { comp: this.#newViewComp(ctrl, this), container: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	#addFieldsetContainerImpl(typeId, props, configCb = null) {
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl("fieldset", typeId, props);
		if (props.title && props.title.length > 0) {
			let legend = document.createElement("legend");
			legend.innerHTML = props.title;
			ctrl.append(legend);
		}

		if (configCb) {
			this.#callConfig(configCb, { comp: this.#newViewComp(ctrl, this), fieldset: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	#callConfig(configCb, args) {
		if (!args.comp) { args.comp = this };
		configCb(args);
	}

	#finished(ctrl, props) {
		if (this.listener) {
			this.listener(this, ctrl, props);
		}
	}

	getElement() {
		return this.elem;
	}

	setListener(cb) {
		this.listener = cb;
		return this;
	}

	config(cb) {
		cb(this);
		return this;
	}

	setForAttributeOn(label, elem) {
		label.htmlFor = elem.id;
		return this;
	}

	/**
	 * PUBLIC
	 * chainable user methods to create the view structure and elements
	 */
	style(styleProps) {
		if (styleProps) {
			this.#setStyleOf(this.elem, styleProps);
		}
		return this;
	}

	attrib(attribProps) {
		if (attribProps) {
			this.#setAttributesOf(this.elem, attribProps);
		}
		return this;
	}

	appendTo(parent) {
		parent.append(this.elem);
		return this;
	}

	prependTo(parent) {
		parent.prepend(this.elem);
		return this;
	}

	addElement(elemType, props, configCb = null) {
		return this.#addElementImpl(elemType, props, configCb);
	}

	addHtml(html, props = {}) {
		let template = document.createElement("template");
		template.innerHTML = html;

		let elements = [...template.content.childNodes].filter(n => n.nodeType === Node.ELEMENT_NODE);
		for (const element of elements) {
			this.#doCtrlAddingTo(this.elem, element, props);
		}
		return this;
	}

	addContainer(props, configCb = null) {
		return this.#addContainerImpl("container", props, configCb);
	}

	addRowContainer(props, configCb = null) {
		return this.#addContainerImpl("rowContainer", props, configCb);
	}

	addColContainer(props, configCb = null) {
		return this.#addContainerImpl("colContainer", props, configCb);
	}

	addFieldset(props, configCb = null) {
		return this.#addFieldsetContainerImpl("fieldset", props, configCb);
	}

	addTitledFieldset(props, configCb = null) {
		return this.#addFieldsetContainerImpl("titledFieldset", props, configCb);
	}

	addLabel(props, configCb = null) {
		let typeId = "label";
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl("label", typeId, props);
		ctrl.innerHTML = props.text;

		if (configCb) {
			this.#callConfig(configCb, { label: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	addLink(props, configCb = null) {
		let typeId = "link";
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl("a", typeId, props);
		ctrl.innerHTML = props.text;

		if (configCb) {
			this.#callConfig(configCb, { link: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	addList(props, configCb = null) {
		let typeId = "list";
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl(getOrDefault(props, "elemType", "ul"), typeId, props);

		if (configCb) {
			this.#callConfig(configCb, { list: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	addTextField(props, configCb = null) {
		let typeId = "textField";
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl("input", typeId, props);
		ctrl.type = "text";
		ctrl.id = this.#reworkId(props.id);

		if (this.#isReadOnly(props)) {
			ctrl.classList.add(this.#getDefaultCompProps().getClassesFor("inputReadOnly"));
			ctrl.disabled = true;
		}

		if (props.datalist) {
			let datalist = this.#newDataList(ctrl, props.datalist);
			this.elem.prepend(datalist.listElem);
		}

		if (configCb) {
			this.#callConfig(configCb, { textfield: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	addButton(props, configCb = null) {
		let typeId = "button";
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl("button", typeId, props);
		ctrl.type = "button";
		ctrl.id = this.#reworkId(props.id);
		ctrl.title = props?.title;

		if (props.iconName) {
			let iconClasses = [...Icons.getIconClasses(props.iconName), "wkv-button-icon"];
			this.#setClassesOf(ctrl, iconClasses);
		}
		ctrl.innerHTML = props.text;

		if (configCb) {
			this.#callConfig(configCb, { button: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	addActionIcon(props, configCb = null) {
		let typeId = "actionIcon";
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl("i", typeId, props);
		ctrl.title = props?.title;
		let iconElement = this.#setupIconFor(ctrl, props);

		if (configCb) {
			this.#callConfig(configCb, { icon: ctrl, "iconElement": iconElement });
		}
		this.#finished(ctrl, props)
		return this;
	}

	addTextArea(props, configCb = null) {
		let typeId = "textArea";
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl("textarea", typeId, props);
		ctrl.id = this.#reworkId(props.id);
		ctrl.rows = props.rows;

		if (this.#isReadOnly(props)) {
			ctrl.classList.add(this.#getDefaultCompProps().getClassesFor("textareaReadOnly"));
			ctrl.disabled = true;
		}

		if (configCb) {
			this.#callConfig(configCb, { textarea: ctrl });
		}
		this.#finished(ctrl, props)
		return this;
	}

	addSeparator(props, configCb = null) {
		let typeId = "hr";
		this.#checkAndReworkArgs([props, configCb], this.#getDefaultCompPropsFor(typeId), (args) => {
			[props, configCb] = args;
		});

		let ctrl = this.#addCtrlImpl("hr", typeId, props);

		if (configCb) {
			this.#callConfig(configCb, { separator: ctrl });
		}
		return this;
	}

	addLabelTextField(lbProps, tfProps, configCb = null) {
		let elems = { label: null, textfield: null };

		this.addLabel(lbProps, (target) => { elems.label = target.label });
		this.addTextField(tfProps, (target) => { elems.textfield = target.textfield });
		this.setForAttributeOn(elems.label, elems.textfield);

		if (configCb) {
			this.#callConfig(configCb, elems);
		}
		return this;
	}

	addLabelTextArea(lbProps, taProps, configCb = null) {
		let elems = { label: null, textarea: null };

		this.addLabel(lbProps, (target) => { elems.label = target.label });
		this.addTextArea(taProps, (target) => { elems.textarea = target.textarea });
		this.setForAttributeOn(elems.label, elems.textarea);

		if (configCb) {
			this.#callConfig(configCb, elems);
		}
		return this;
	}

	addLabelButton(lbProps, pbProps, configCb = null) {
		let elems = { label: null, button: null };

		this.addLabel(lbProps, (target) => { elems.label = target.label });
		this.addButton(pbProps, (target) => { elems.button = target.button });

		if (configCb) {
			this.#callConfig(configCb, elems);
		}
		return this;
	}

}

