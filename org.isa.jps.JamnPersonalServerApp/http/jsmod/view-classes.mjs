/* Authored by iqbserve.de */

import { getChildOf, setVisibility, setDisplay, typeUtil, fileUtil } from '../jsmod/tools.mjs';
import { ViewSource } from '../jsmod/data-classes.mjs';

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

	static newIcon(name, elem, type = "bi") {
		return new IconElement(elem, type, IconElement.#Icons[type][name]).init(IconElement.#Icons[type].defaultInit);
	}

	static iconDef(name, type = "bi") {
		return IconElement.#Icons[type][name];
	}

	static #Icons = {
		bi: {
			defaultInit: (iconObj) => {
				//Bootstrap font icons
				iconObj.elem.classList.add(iconObj.type ? iconObj.type : "bi");
				iconObj.elem.classList.add(iconObj.shapes[0]);
			},
			close: ["bi-x-lg", ""],
			pin: ["bi-pin", "bi-pin-angle"],
			collapse: ["bi-chevron-bar-contract", "bi-chevron-bar-expand"],
			dotmenu: ["bi-three-dots-vertical", ""],
			menu: ["bi-list", ""],
			login: ["bi-person", "bi-person-check"],
			github: ["bi-github", ""],
			system: ["bi-laptop", ""],
			command: ["bi-command", ""],
			tools: ["bi-tools", ""],
			user: ["bi-person", ""],
			password: ["bi-key", ""],
			loginAction: ["bi-box-arrow-in-right", ""],
			tableSort: ["bi-arrow-down", "bi-arrow-up"],
			save: ["bi-floppy", ""],
			redo: ["bi-arrow-counterclockwise", ""],
			plusNew: ["bi-plus-square", ""],
			minusRemove: ["bi-dash-square", ""],
			xRemove: ["bi-x-square", ""],
			trash: ["bi-trash", ""],
			clipboardAdd: ["bi-clipboard-plus", ""],
			eraser: ["bi-eraser", ""],
			caretup: ["bi-caret-up", ""],
			caretdown: ["bi-caret-down", ""],
			run: ["bi-caret-right-square", ""]
		}
	}
}

/**
 * A basic view class. 
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
		this.isOpen = true;
	}

	setVisible(flag) {
		setVisibility(this.viewElement, flag);
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
	headerMenu = null;
	workIndicator = null;
	menuIcon = null;
	closeIcon = null;
	collapseIcon = null;
	pinIcon = null;

	isInitialized = false;
	isRunning = false;
	isOpen = false;
	isPinned = false;
	isCollapsed = false;

	workAreaDisplay;

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
		this.workAreaDisplay = this.viewWorkarea.style.display;
		this.workIndicator = this.getElement("work.indicator");

		this.closeIcon = IconElement.newIcon("close", this.getElement("close.icon"));
		this.pinIcon = IconElement.newIcon("pin", this.getElement("pin.icon"));
		this.collapseIcon = IconElement.newIcon("collapse", this.getElement("collapse.icon"));
		this.menuIcon = IconElement.newIcon("dotmenu", this.getElement("menu.icon"));

		this.headerMenu = new WorkViewHeaderMenu(this.getElement("header.menu"));

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

	open(data = null) {
		if (!this.isInitialized) {
			this.initialize();
		}
		this.headerMenu.close();
		this.isOpen = true;
	}

	setVisible(flag) {
		setVisibility(this.viewElement, flag);
	}

	close() {
		if (this.isInitialized) {
			this.isOpen = false;
			this.headerMenu.close();
		}
		return this.isCloseable();
	}

	isCloseable(ctxObj = null) {
		return !(this.isRunning || this.isPinned);
	}

	setRunning(flag) {
		this.isRunning = flag;
		setVisibility(this.workIndicator, flag);
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
			let displayVal = !this.isCollapsed ? this.workAreaDisplay : "none";
			setDisplay(this.viewWorkarea, displayVal);
		});

		return this.isCollapsed;
	}

	toggleHeaderMenu() {
		if (!this.isCollapsed) {
			this.headerMenu.toggleVisibility();
		}
	}

	statusLineInfo(info) {
		WbApp.statusLineInfo(info);
	}

	copyToClipboard(text) {
		if (!this.isRunning && (text && text.length > 0)) {
			navigator.clipboard.writeText(text);
		}
	}

	saveToFile(fileName, text) {
		if (!this.isRunning && text.length > 0) {
			fileUtil.saveToFileClassic(fileName, text);
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
		this.dialogElem = document.getElementById("standardDialog");
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
				col.onclick = (evt) => { this.tableData.cellClick(rowKey, colKey, evt); };
				col.ondblclick = (evt) => { this.tableData.cellDblClick(rowKey, colKey, evt); };
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
			ctrl.onclick = (evt) => { ctrl.value = typeUtil.stringFromBoolean(ctrl.checked) };
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

export class SplitBarHandler {

	splitter;
	compBefore;
	compAfter;
	orientation = "v";

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

		this.splitter.style.left = val + "px";
		this.compBefore.style.width = (this.clickPoint.beforeWidth + delta.x) + "px";
		this.compAfter.style.width = (this.clickPoint.afterWidth - delta.x) + "px";
	}

	stop() {
		document.dispatchEvent(new Event("mouseup", { bubbles: true, cancelable: true }));
	}
}

/*********************************************************************************
 * UI BUILDER CLASSES
 *********************************************************************************/
class DefaultCompCSSClasses {
	compSet = "wkv-compset";
	comp = "wkv-ctrlcomp";
	label = "wkv-label-ctrl";
	link = "wkv-link-ctrl";
	list = "wkv-list-ctrl";
	actionIcon = "wkv-header-action-ctrl";
	button = "wkv-button-ctrl";
	textField = "wkv-value-ctrl";
	textArea = "wkv-textarea-ctrl";
	inputReadOnly = "input-readonly";
	textareaReadOnly = "textarea-readonly";
	hr = "solid";

	getFor(id) {
		return this[id];
	}
}

export class DataList {
	ctrl;
	element;

	constructor(ctrl) {
		this.ctrl = ctrl;
		this.element = document.createElement("datalist");
		this.element.id = "data." + ctrl.id;
		this.ctrl.setAttribute("list", this.element.id);
	}

	#newOption(item) {
		let option = document.createElement("option");
		option.id = item.id ? item.id : item;
		option.value = item.value ? item.value : option.id;
		return option;
	}

	setOptions(data) {
		let option = null;
		data.forEach(item => {
			option = this.#newOption(item);
			this.element.append(option);
		});
	}

	removeOption(id) {
		ViewBuilder.removeChildFrom(this.element, id);
	}

	addOption(item) {
		let id = item.id ? item.id : item;
		let option = ViewBuilder.getChildFrom(this.element, id);
		if (option === null) {
			option = this.#newOption(item);
			this.element.prepend(option);
		}
	}
}

/**
 * An experimental factory/builder to create standard UI components e.g. like
 *  [label] - [textfield] etc.
 * arranged e.g. in a fieldset container.
 * 
 * The builder just provides the basic html elements
 * and returns a "proxy" object (ViewComp) that provides styling, attribution etc. methods
 * to enable a cascading builder style programming format.
 */
export class ViewBuilder {

	defaultCSSClasses = new DefaultCompCSSClasses();
	defaultStyles = {
	}
	elementCollection = {};
	objectCollection = {};

	static #valueClearableInputTypes = ["text", "password"];
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
		if (Array.isArray(clazzes)) {
			clazzes.forEach(clazz => ctrl.classList.add(clazz));
		} else if (clazzes) {
			ctrl.classList.add(clazzes);
		} else if (defaultClazzes) {
			ViewBuilder.setClassesOf(ctrl, defaultClazzes, null);
		}
	}

	static setStyleOf(ctrl, styleProps) {
		if (styleProps) {
			for (const name in styleProps) {
				ctrl.style[name] = styleProps[name];
			}
		}
	}

	static setAttributesOf(ctrl, attributeProps) {
		for (const name in attributeProps) {
			ctrl[name] = attributeProps[name];
		}
	}

	static mergeProps(propsA, propsB) {
		let props = { ...propsB, ...propsA };
		return props;
	}

	setElementCollection(obj) {
		this.elementCollection = obj;
	}

	setObjectCollection(obj) {
		this.objectCollection = obj;
	}

	getCtrl(varid) {
		return this.elementCollection[varid];
	}

	getDefaultCSSClassFor(id) {
		return this.defaultCSSClasses.getFor(id);
	}

	newFieldset(props = { title: "", clazzes: "wkv-compset", styleProps: {} }) {
		let fieldset = document.createElement("fieldset");

		ViewBuilder.setClassesOf(fieldset, this.getDefaultCSSClassFor("compSet"));
		ViewBuilder.setClassesOf(fieldset, props.clazzes);
		ViewBuilder.setStyleOf(fieldset, props.styleProps);

		if (props.title && props.title.length > 0) {
			let legend = document.createElement("legend");
			legend.innerHTML = props.title;
			fieldset.append(legend);
		}
		return fieldset;
	}

	newViewComp(props = null) {
		if (props) {
			return new ViewComp(this, props);
		}
		return new ViewComp(this);
	}
}

/**
 * The component object provides the preconfigured dom elements
 * and methods for "configuration/appending" of the ui controls. 
 */
export class ViewComp {

	#directlySupportedAttributes = ["innerHTML", "disabled"];

	builder;
	comp = null;

	constructor(builder, props = { clazzes: "wkv-ctrlcomp", type: "row" }) {
		this.builder = builder;
		if (props) {
			this.comp = document.createElement("span");
			this.#setClassesOf(this.comp, props.clazzes, "comp");
			this.#applyDefaultStyles(this.comp, "comp");
		}
	}

	static newFor(element) {
		return new ViewComp(new ViewBuilder(), null).setComp(element);
	}

	setComp(comp) {
		this.comp = comp;
		return this;
	}

	#reworkId(id) {
		return ViewBuilder.reworkId(id);
	}

	#setClassesOf(ctrl, clazzes, defaultId) {
		ViewBuilder.setClassesOf(ctrl, clazzes, this.builder.getDefaultCSSClassFor(defaultId));
	}

	#setStyleOf(ctrl, styleProps) {
		ViewBuilder.setStyleOf(ctrl, styleProps);
	}

	#setAttributesOf(ctrl, attribProps) {
		ViewBuilder.setAttributesOf(ctrl, attribProps);
	}

	#applyDefaultStyles(ctrl, typeId) {
		if (this.builder.defaultStyles[typeId]) {
			this.#setStyleOf(ctrl, this.builder.defaultStyles[typeId]);
		}
	}

	#applyDirectAttributeProperties(ctrl, props) {
		//comfort method
		//apply the list of direct supported attributes if any in props
		let directPropValues = {};
		this.#directlySupportedAttributes.forEach((name) => {
			if (props.hasOwnProperty(name)) {
				directPropValues[name] = props[name];
			}
		});
		if (Object.keys(directPropValues).length > 0) {
			this.#setAttributesOf(ctrl, directPropValues);
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

	#registerCtrl(varid, ctrl) {
		if (varid && this.builder.elementCollection) {
			this.builder.elementCollection[varid] = ctrl;
		}
		this.#registerObject(ctrl);
	}

	#registerObject(obj, id = null) {
		if (this.builder.objectCollection) {
			if (id) {
				this.builder.objectCollection[id] = obj;
			} else if (obj.hasOwnProperty("data-bind")) {
				if (!this.builder.objectCollection.bindings) {
					this.builder.objectCollection.bindings = {};
				}
				let key = obj["data-bind"];
				this.builder.objectCollection.bindings[key] = obj;
			}
		}
	}

	#appendCtrl(props, ctrl) {
		if (props.parentCtrl) {
			props.parentCtrl.append(ctrl);
		} else {
			this.comp.append(ctrl);
		}
	}

	#isReadOnly(props) {
		return props.hasOwnProperty('readOnly');
	}

	#newDataList(ctrl, data) {
		let datalist = new DataList(ctrl);
		datalist.setOptions(data);
		this.#registerObject(datalist, datalist.element.id);
		return datalist;
	}

	ctrl(varid, cb) {
		cb(this.getCtrl(varid), this);
		return this;
	}

	getCtrl(varid) {
		return this.builder.elementCollection[varid];
	}

	style(styleProps) {
		if (styleProps) {
			this.#setStyleOf(this.comp, styleProps);
		}
		return this;
	}

	attrib(attribProps) {
		if (attribProps) {
			this.#setAttributesOf(this.comp, attribProps);
		}
		return this;
	}

	innerHTML(html) {
		this.comp.innerHTML = html;
		return this;
	}

	appendTo(parent) {
		parent.append(this.comp);
		return this;
	}

	prependTo(parent) {
		parent.prepend(this.comp);
		return this;
	}

	config(configCb) {
		configCb(this);
		return this;
	}

	addSeparator(props = { clazzes: "" }, configCb = null) {
		let typeId = "hr";
		let ctrl = document.createElement("hr");
		this.#setClassesOf(ctrl, props.clazzes, typeId);

		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (configCb) {
			configCb({ comp: this, container: ctrl });
		}

		return this;
	}

	addContainer(props = { clazzes: "", type: "span" }, configCb = null) {
		let typeId = props.type ? props.type : "span";
		let ctrl = document.createElement(typeId);
		this.#setClassesOf(ctrl, props.clazzes, typeId);

		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (configCb) {
			configCb({ comp: this, container: ctrl });
		}

		return this;
	}

	addLabel(props = { text: "unknown", clazzes: "" }, configCb = null) {
		let typeId = "label";
		let ctrl = document.createElement("label");
		this.#setClassesOf(ctrl, props.clazzes, typeId);
		ctrl.innerHTML = props.text;

		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (configCb) {
			configCb({ comp: this, label: ctrl });
		}

		return this;
	}

	addLink(props = { text: "unknown", clazzes: "" }, configCb = null) {
		let typeId = "link";
		let ctrl = document.createElement("a");
		this.#setClassesOf(ctrl, props.clazzes, typeId);
		ctrl.innerHTML = props.text;

		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (configCb) {
			configCb({ comp: this, link: ctrl });
		}

		return this;
	}

	addList(props = { clazzes: "", type: "ul" }, configCb = null) {
		let typeId = "list";
		let ctrl = document.createElement(props.type ? props.type : "ul");
		this.#setClassesOf(ctrl, props.clazzes, typeId);

		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (configCb) {
			configCb({ comp: this, list: ctrl });
		}

		return this;
	}

	addTextField(props = { varid: "", id: "", clazzes: "" }, configCb = null) {
		let typeId = "textField";
		let ctrl = document.createElement("input");
		ctrl.type = "text";
		ctrl.id = this.#reworkId(props.id);
		this.#setClassesOf(ctrl, props.clazzes, typeId);

		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		if (this.#isReadOnly(props)) {
			ctrl.classList.add(this.builder.getDefaultCSSClassFor("inputReadOnly"));
			ctrl.disabled = true;
		}

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (props.datalist) {
			let datalist = this.#newDataList(ctrl, props.datalist);
			this.comp.prepend(datalist.element);
		}

		if (configCb) {
			configCb({ comp: this, textfield: ctrl });
		}

		return this;
	}

	addButton(props = { text: "", title: "", icon: null, varid: "", id: "", clazzes: "" }, configCb = null) {
		let typeId = "button";
		let ctrl = document.createElement("button");
		ctrl.type = "button";
		ctrl.id = this.#reworkId(props.id);
		ctrl.title = props?.title;

		if (props.icon) {
			let iconClasses = [IconElement.iconDef(props.icon)[0], "wkv-button-icon"];
			this.#setClassesOf(ctrl, iconClasses);
		}

		ctrl.innerHTML = props.text;
		this.#setClassesOf(ctrl, props.clazzes, typeId);

		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (configCb) {
			configCb({ comp: this, button: ctrl });
		}

		return this;
	}

	addActionIcon(props = { varid: "", iconName: "", title: "", clazzes: "" }, configCb = null) {
		let typeId = "actionIcon";
		let ctrl = document.createElement("i");
		this.#setClassesOf(ctrl, props.clazzes, typeId);
		ctrl.setAttribute("title", props?.title);
		IconElement.newIcon(props.iconName, ctrl);

		this.#applyProperties(ctrl, props)

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (configCb) {
			configCb({ comp: this, icon: ctrl });
		}

		return this;
	}

	addTextArea(props = { varid: "", id: "", clazzes: "" }, configCb = null) {
		let typeId = "textArea";
		let ctrl = document.createElement("textarea");
		ctrl.id = this.#reworkId(props.id);
		ctrl.rows = props.rows;
		this.#setClassesOf(ctrl, props.clazzes, typeId);

		this.#applyDefaultStyles(ctrl, typeId);
		this.#applyProperties(ctrl, props)

		if (this.#isReadOnly(props)) {
			ctrl.classList.add(this.builder.getDefaultCSSClassFor("textareaReadOnly"));
			ctrl.disabled = true;
		}

		this.#appendCtrl(props, ctrl);
		this.#registerCtrl(props.varid, ctrl);

		if (configCb) {
			configCb({ comp: this, textarea: ctrl });
		}

		return this;
	}

	addLabelTextField(lbProps, tfProps, configCb = null) {
		let elems = { comp: this, label: null, textfield: null };

		this.addLabel(lbProps, (target) => { elems.label = target.label });
		this.addTextField(tfProps, (target) => { elems.textfield = target.textfield });
		elems.label.htmlFor = elems.textfield.id;

		if (configCb) {
			configCb(elems);
		}

		return this;
	}

	addLabelTextArea(lbProps, taProps, configCb = null) {
		let elems = { comp: this, label: null, textarea: null };

		this.addLabel(lbProps, (target) => { elems.label = target.label });
		this.addTextArea(taProps, (target) => { elems.textarea = target.textarea });
		elems.label.htmlFor = elems.textarea.id;

		if (configCb) {
			configCb(elems);
		}

		return this;
	}

	addLabelButton(lbProps, pbProps, configCb = null) {
		let elems = { comp: this, label: null, button: null };

		this.addLabel(lbProps, (target) => { elems.label = target.label });
		this.addButton(pbProps, (target) => { elems.button = target.button });

		if (configCb) {
			configCb(elems);
		}

		return this;
	}

}

